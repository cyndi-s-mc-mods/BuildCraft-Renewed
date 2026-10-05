/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.BuildCraft;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.transport.BCTransportConfig;

/** Powers the pipe it's on (like a wooden pipe) in pulses, so no engine is needed. Right click to turn it on and off,
 * or control it with a gate. */
public class PluggablePulsar extends PipePluggable {
    private static final int PULSE_STAGE = 20;
    private static final Identifier STATIC = BuildCraft.id("block/plugs/pulsar_static");
    private static final Identifier ON = BuildCraft.id("block/plugs/pulsar_dynamic_on");
    private static final Identifier OFF = BuildCraft.id("block/plugs/pulsar_dynamic_off");

    private boolean manuallyEnabled = false;
    private int pulseStage = 0;
    private int gateEnabledTicks;
    private int gateSinglePulses;
    private boolean lastPulsing = false;

    public PluggablePulsar(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    public static PluggablePulsar load(PluggableDefinition definition, IPipeHolder holder, Direction side, ValueInput input) {
        PluggablePulsar pulsar = new PluggablePulsar(definition, holder, side);
        pulsar.manuallyEnabled = input.getBooleanOr("manuallyEnabled", false);
        pulsar.gateEnabledTicks = input.getIntOr("gateEnabledTicks", 0);
        pulsar.gateSinglePulses = input.getIntOr("gateSinglePulses", 0);
        pulsar.pulseStage = Math.clamp(input.getIntOr("pulseStage", 0), 0, PULSE_STAGE);
        pulsar.lastPulsing = input.getBooleanOr("pulsing", false);
        return pulsar;
    }

    @Override
    public void save(ValueOutput output) {
        output.putBoolean("manuallyEnabled", manuallyEnabled);
        output.putInt("gateEnabledTicks", gateEnabledTicks);
        output.putInt("gateSinglePulses", gateSinglePulses);
        output.putInt("pulseStage", pulseStage);
        output.putBoolean("pulsing", isPulsing());
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(2, 5, 5, 4, 11, 11, side);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.PLUG_PULSAR.get());
    }

    private boolean isPulsing() {
        return manuallyEnabled || gateEnabledTicks > 0 || gateSinglePulses > 0;
    }

    /** Keeps the pulsar on for a little while. Called every tick by the "power pulsar" gate action. */
    public void enablePulsar() {
        gateEnabledTicks = 10;
    }

    public void addSinglePulse() {
        gateSinglePulses++;
    }

    @Override
    public void onTick() {
        if (holder.getPipeWorld().isClientSide()) {
            pulseStage = lastPulsing ? (pulseStage + 1) % PULSE_STAGE : 0;
            return;
        }
        boolean on = isPulsing();
        pulseStage = on ? pulseStage + 1 : 0;
        if (gateEnabledTicks > 0) {
            gateEnabledTicks--;
        }
        if (pulseStage >= PULSE_STAGE) {
            pulseStage = 0;
            if (holder.getPipe().getBehaviour() instanceof IMjRedstoneReceiver receiver) {
                if (gateSinglePulses > 0) {
                    long power = MjAPI.MJ;
                    if (holder.getPipe().getFlow() instanceof IFlowFluid) {
                        power = BCTransportConfig.mjPerMillibucket * 1000;
                    } else if (holder.getPipe().getFlow() instanceof IFlowItems) {
                        power = BCTransportConfig.mjPerItem;
                    }
                    if (receiver.receivePower(power, true) == 0) {
                        receiver.receivePower(power, false);
                        gateSinglePulses--;
                    }
                } else {
                    receiver.receivePower(MjAPI.MJ, false);
                }
            }
        }
        if (on != lastPulsing) {
            lastPulsing = on;
            scheduleNetworkUpdate();
        }
    }

    @Override
    public InteractionResult onPluggableActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!holder.getPipeWorld().isClientSide()) {
            manuallyEnabled = !manuallyEnabled;
            holder.getPipeWorld().playSound(null, holder.getPipePos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f,
                manuallyEnabled ? 0.6f : 0.5f);
            scheduleNetworkUpdate();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<PlugModelPart> getModel() {
        List<PlugModelPart> parts = new ArrayList<>();
        parts.add(SimplePlugModels.plate(STATIC, 2));
        // The moving piston: in and out once a second while pulsing
        double stage = lastPulsing ? pulseStage / (double) PULSE_STAGE : 0;
        double mirrored = 2 * (stage > 0.5 ? 1 - stage : stage);
        float pos = (float) ((1 - mirrored) * 2 - 0.001);
        Identifier tex = lastPulsing ? ON : OFF;
        PlugModelPart.Face side = new PlugModelPart.Face(tex, 4, 6, 6, 10);
        PlugModelPart.Face end = new PlugModelPart.Face(tex, 6, 6, 10, 10);
        parts.add(PlugModelPart.box(pos, 6, 6, pos + 2, 10, 10, new PlugModelPart.Face[] { side, side, side, side, end, end }));
        return parts;
    }
}
