/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemPluggableGate;

/** A gate on the side of a pipe. */
public class PluggableGate extends PipePluggable implements MenuProvider {
    public final GateLogic logic;

    public PluggableGate(PluggableDefinition definition, IPipeHolder holder, Direction side, GateVariant variant) {
        super(definition, holder, side);
        this.logic = new GateLogic(this, variant);
    }

    public static PluggableGate load(PluggableDefinition definition, IPipeHolder holder, Direction side, ValueInput input) {
        GateVariant variant = input.read("variant", GateVariant.CODEC).orElse(GateVariant.BASIC);
        PluggableGate gate = new PluggableGate(definition, holder, side, variant);
        gate.logic.load(input);
        return gate;
    }

    @Override
    public void save(ValueOutput output) {
        output.store("variant", GateVariant.CODEC, logic.variant);
        logic.save(output);
    }

    @Override
    public void onTick() {
        if (holder.getPipeWorld().isClientSide()) return;
        if (logic.resolveActions()) {
            scheduleNetworkUpdate();
        }
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(2, 5, 5, 4, 11, 11, side);
    }

    @Override
    public ItemStack getPickStack() {
        return ItemPluggableGate.getStack(logic.variant);
    }

    @Override
    public InteractionResult onPluggableActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canConnectToRedstone() {
        return true;
    }

    @Override
    public int getRedstoneOutput(Direction pipeSide) {
        return logic.getRedstoneOutput(pipeSide);
    }

    @Override
    public boolean isEmittingWire(net.minecraft.world.item.DyeColor colour) {
        return logic.isEmitting(colour);
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return logic.variant.getDisplayName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerGate(id, inventory, this);
    }

    // Model

    private static final Identifier GATE_ON = BuildCraft.id("block/gates/gate_on");
    private static final Identifier GATE_OFF = BuildCraft.id("block/gates/gate_off");

    public static Identifier materialTexture(GateVariant.Material material) {
        return switch (material) {
            case CLAY_BRICK -> Identifier.withDefaultNamespace("block/bricks");
            case IRON -> Identifier.withDefaultNamespace("block/iron_block");
            case NETHER_BRICK -> Identifier.withDefaultNamespace("block/nether_bricks");
            case GOLD -> Identifier.withDefaultNamespace("block/gold_block");
        };
    }

    public static Identifier modifierTexture(GateVariant.Modifier modifier) {
        return switch (modifier) {
            case LAPIS -> Identifier.withDefaultNamespace("block/lapis_block");
            case QUARTZ -> Identifier.withDefaultNamespace("block/quartz_block_top");
            default -> Identifier.withDefaultNamespace("block/diamond_block");
        };
    }

    private static PlugModelPart part(float x0, float y0, float z0, float x1, float y1, float z1, Identifier texture) {
        PlugModelPart.Face end = new PlugModelPart.Face(texture, 5, 5, 11, 11);
        PlugModelPart.Face side = new PlugModelPart.Face(texture, 2, 5, 4, 11);
        return PlugModelPart.box(x0, y0, z0, x1, y1, z1, new PlugModelPart.Face[] { side, side, side, side, end, end });
    }

    @Override
    public List<PlugModelPart> getModel() {
        return getModel(logic.variant, logic.isOn);
    }

    public static List<PlugModelPart> getModel(GateVariant variant, boolean on) {
        List<PlugModelPart> parts = new ArrayList<>();
        parts.add(part(2, 5, 5, 4.01f, 11, 11, materialTexture(variant.material())));
        parts.add(part(1.9f, 6, 6, 4.1f, 10, 10, on ? GATE_ON : GATE_OFF));
        if (variant.material() != GateVariant.Material.CLAY_BRICK) {
            parts.add(part(1.8f, 7, 7, 4.2f, 9, 9, BuildCraft.id("block/gates/gate_" + variant.logic().getSerializedName())));
        }
        if (variant.modifier() != GateVariant.Modifier.NO_MODIFIER) {
            Identifier texture = modifierTexture(variant.modifier());
            parts.add(part(1.8f, 5.5f, 5.5f, 4.2f, 6.5f, 6.5f, texture));
            parts.add(part(1.8f, 9.5f, 5.5f, 4.2f, 10.5f, 6.5f, texture));
            parts.add(part(1.8f, 5.5f, 9.5f, 4.2f, 6.5f, 10.5f, texture));
            parts.add(part(1.8f, 9.5f, 9.5f, 4.2f, 10.5f, 10.5f, texture));
        }
        return parts;
    }
}
