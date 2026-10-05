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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.transport.block.BlockPipe;

/** Pipes that point at one side, which can be changed with a wrench. */
public abstract class PipeBehaviourDirectional extends PipeBehaviour {
    protected EnumPipePart currentDir = EnumPipePart.CENTER;

    public PipeBehaviourDirectional(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        output.putInt("currentDir", currentDir.ordinal());
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        currentDir = EnumPipePart.VALUES[Math.floorMod(input.getIntOr("currentDir", EnumPipePart.CENTER.ordinal()), 7)];
    }

    @Override
    public BlockState updateBlockState(BlockState state) {
        if (state.hasProperty(BlockPipe.DIR)) {
            state = state.setValue(BlockPipe.DIR, currentDir);
        }
        return state;
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (held.getItem() instanceof IToolWrench) {
            if (!player.level().isClientSide()) {
                if (part == EnumPipePart.CENTER) {
                    advanceFacing();
                } else if (part.face != getCurrentDir() && canFaceDirection(part.face)) {
                    setCurrentDir(part.face);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onTick() {
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            return;
        }
        if (!canFaceDirection(getCurrentDir())) {
            if (!advanceFacing()) {
                setCurrentDir(null);
            }
        }
    }

    protected abstract boolean canFaceDirection(@Nullable Direction dir);

    public boolean advanceFacing() {
        Direction current = currentDir.face == null ? Direction.EAST : currentDir.face;
        for (int i = 0; i < 6; i++) {
            current = RotationUtil.next(current);
            if (canFaceDirection(current)) {
                setCurrentDir(current);
                return true;
            }
        }
        return false;
    }

    @Nullable
    protected Direction getCurrentDir() {
        return currentDir.face;
    }

    protected void setCurrentDir(@Nullable Direction setTo) {
        if (this.currentDir.face == setTo) {
            return;
        }
        this.currentDir = EnumPipePart.fromFacing(setTo);
        if (!pipe.getHolder().getPipeWorld().isClientSide()) {
            pipe.getHolder().scheduleBlockStateUpdate();
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }
}
