/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Stripes pipes break the block in front of their open end when powered, and place blocks that reach that end. */
public class PipeBehaviourStripes extends PipeBehaviour implements IMjRedstoneReceiver {
    private final MjBattery battery = new MjBattery(256 * MjAPI.MJ);
    public @Nullable Direction direction = null;
    private long progress;

    public PipeBehaviourStripes(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        battery.save(output, "battery");
        output.putInt("direction", direction == null ? -1 : direction.get3DDataValue());
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        battery.load(input, "battery");
        int dir = input.getIntOr("direction", -1);
        direction = dir < 0 || dir > 5 ? null : Direction.from3DDataValue(dir);
    }

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        return !(other instanceof PipeBehaviourStripes);
    }

    @Override
    public void onTick() {
        Level level = pipe.getHolder().getPipeWorld();
        BlockPos pos = pipe.getHolder().getPipePos();
        if (level.isClientSide()) {
            return;
        }
        if (direction == null || pipe.isConnected(direction)) {
            int sides = 0;
            Direction dir = null;
            for (Direction face : Direction.values()) {
                if (pipe.isConnected(face)) {
                    sides++;
                    dir = face;
                }
            }
            direction = sides == 1 ? dir.getOpposite() : null;
        }
        battery.tick();
        if (direction == null) {
            progress = 0;
            return;
        }
        BlockPos offset = pos.relative(direction);
        BlockState state = level.getBlockState(offset);
        float hardness = state.getDestroySpeed(level, offset);
        if (state.isAir() || hardness < 0 || state.liquid()) {
            progress = 0;
            return;
        }
        long target = (long) ((hardness + 1) * 2 * MjAPI.MJ);
        if (progress < target) {
            progress += battery.extractPower(0, Math.min(target - progress, MjAPI.MJ * 10));
            if (progress > 0) {
                level.destroyBlockProgress(offset.hashCode(), offset, (int) (progress * 9 / target));
            }
        } else {
            level.destroyBlockProgress(offset.hashCode(), offset, -1);
            if (level instanceof ServerLevel serverLevel) {
                List<ItemStack> drops = Block.getDrops(state, serverLevel, offset, level.getBlockEntity(offset), null,
                    new ItemStack(Items.DIAMOND_PICKAXE));
                level.destroyBlock(offset, false);
                for (ItemStack stack : drops) {
                    sendItem(stack, direction);
                }
            }
            progress = 0;
        }
    }

    /** Places block items that reach the open end, instead of dropping them. */
    @PipeEventHandler
    public void onDrop(PipeEventItem.Drop event) {
        if (direction == null) {
            return;
        }
        ItemStack stack = event.getStack();
        if (stack.getItem() instanceof BlockItem blockItem) {
            Level level = pipe.getHolder().getPipeWorld();
            BlockPos target = pipe.getHolder().getPipePos().relative(direction);
            if (level.getBlockState(target).canBeReplaced()) {
                BlockState place = blockItem.getBlock().defaultBlockState();
                if (place.canSurvive(level, target) && level.setBlock(target, place, Block.UPDATE_ALL)) {
                    stack.shrink(1);
                    event.setStack(stack);
                }
            }
        }
    }

    private void sendItem(ItemStack stack, Direction from) {
        if (pipe.getFlow() instanceof IFlowItems flow) {
            flow.insertItemsForce(stack, from, null, 0.02);
        }
    }

    // MJ

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return this;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return battery.getCapacity() - battery.getStored();
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        return battery.addPowerChecking(microJoules, simulate);
    }
}
