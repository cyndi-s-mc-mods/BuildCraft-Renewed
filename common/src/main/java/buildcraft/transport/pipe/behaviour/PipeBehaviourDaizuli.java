/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
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
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.transport.block.BlockPipe;

/** Daizuli pipes send items of their colour out of the side they point at, and everything else elsewhere. */
public class PipeBehaviourDaizuli extends PipeBehaviourDirectional implements IColouredPipe {
    private DyeColor colour = DyeColor.WHITE;

    public PipeBehaviourDaizuli(IPipe pipe) {
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
        state = super.updateBlockState(state);
        return state.hasProperty(BlockPipe.COLOUR) ? state.setValue(BlockPipe.COLOUR, colour) : state;
    }

    @Override
    protected boolean canFaceDirection(@Nullable Direction dir) {
        return dir != null;
    }

    @Override
    public void setColour(DyeColor colour) {
        if (this.colour == colour) return;
        this.colour = colour;
        pipe.getHolder().scheduleBlockStateUpdate();
        pipe.getHolder().scheduleNetworkUpdate();
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (part != EnumPipePart.CENTER && part != currentDir) {
            return super.onPipeActivate(player, hand, held, hit, part);
        }
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
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        Direction dir = currentDir.face;
        if (dir == null) return;
        if (colour == sideCheck.colour) {
            sideCheck.disallowAllExcept(dir);
        } else {
            sideCheck.disallow(dir);
        }
    }
}
