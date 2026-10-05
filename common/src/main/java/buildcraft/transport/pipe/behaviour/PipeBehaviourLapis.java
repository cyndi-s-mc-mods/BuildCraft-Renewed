/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.transport.block.BlockPipe;

/** Lapis pipes paint the items that go through them. Wrench them to change the colour. */
public class PipeBehaviourLapis extends PipeBehaviour {
    private DyeColor colour = DyeColor.WHITE;

    public PipeBehaviourLapis(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        output.putInt("colour", colour.getId());
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        colour = DyeColor.byId(input.getIntOr("colour", 0));
    }

    @Override
    public BlockState updateBlockState(BlockState state) {
        return state.hasProperty(BlockPipe.COLOUR) ? state.setValue(BlockPipe.COLOUR, colour) : state;
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (held.getItem() instanceof IToolWrench) {
            if (!player.level().isClientSide()) {
                colour = DyeColor.byId((colour.getId() + (player.isSecondaryUseActive() ? 15 : 1)) & 15);
                pipe.getHolder().scheduleBlockStateUpdate();
                pipe.getHolder().scheduleNetworkUpdate();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @PipeEventHandler
    public void onReachCenter(PipeEventItem.ReachCenter reachCenter) {
        reachCenter.colour = colour;
    }
}
