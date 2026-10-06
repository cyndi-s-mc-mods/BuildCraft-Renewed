/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IFlowPower;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventPower;
import buildcraft.transport.block.BlockPipe;

/** Iron and diamond kinesis pipes: wrench them to limit how much power goes through. */
public class PipeBehaviourLimiter extends PipeBehaviour {
    public static final int MAX_SHIFT = 6;
    private int limitShift = 0;

    public PipeBehaviourLimiter(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        output.putInt("limitShift", limitShift);
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        limitShift = Mth.clamp(input.getIntOr("limitShift", 0), 0, MAX_SHIFT);
    }

    @Override
    public BlockState updateBlockState(BlockState state) {
        return state.hasProperty(BlockPipe.LIMIT) ? state.setValue(BlockPipe.LIMIT, limitShift) : state;
    }

    public int getLimitShift() {
        return limitShift;
    }

    /** Sets the limit (as a gate does): the power through is divided by 2 to the power of the shift, or none at all for
     * {@link #MAX_SHIFT}. */
    public void setLimitShift(int shift) {
        shift = Mth.clamp(shift, 0, MAX_SHIFT);
        if (shift == limitShift) return;
        limitShift = shift;
        if (pipe.getFlow() instanceof IFlowPower flow) {
            flow.reconfigure();
        }
        pipe.getHolder().scheduleBlockStateUpdate();
        pipe.getHolder().scheduleNetworkUpdate();
    }

    @PipeEventHandler
    public void configurePower(PipeEventPower.Configure event) {
        if (limitShift == MAX_SHIFT) {
            event.disableTransfer();
        } else {
            event.setMaxPower(event.getMaxPower() >> limitShift);
        }
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (!(held.getItem() instanceof IToolWrench)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            limitShift = (limitShift + 1) % (MAX_SHIFT + 1);
            long limit = limitShift == MAX_SHIFT ? 0
                : (PipeApi.getPowerTransferInfo(pipe.getDefinition()).transferPerTick() >> limitShift) / MjAPI.MJ;
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.pipe.power.limit", limit));
            if (pipe.getFlow() instanceof IFlowPower flow) {
                flow.reconfigure();
            }
            pipe.getHolder().scheduleBlockStateUpdate();
            pipe.getHolder().scheduleNetworkUpdate();
        }
        return InteractionResult.SUCCESS;
    }
}
