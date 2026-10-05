/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.InventoryUtil;

/** Digs straight down, one block at a time, putting what it mines into adjacent pipes or inventories. */
public class TileMiningWell extends TileMiner {
    /** How often an idle mining well looks for something new to mine. */
    private static final int RECHECK_INTERVAL = 40;
    private int idleTicks = 0;
    /** Where the mining well stopped digging, so the tube can stay down the hole once it has finished. */
    @Nullable
    private BlockPos stopPos;

    public TileMiningWell(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.MINING_WELL_TILE.get(), pos, state);
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjBatteryReceiver(battery);
    }

    @Override
    protected void mine() {
        if (!(level instanceof ServerLevel server)) return;
        if (currentPos != null && canBreak(currentPos)) {
            long target = BlockUtil.computeBlockBreakPower(server, currentPos);
            progress += (int) battery.extractPower(0, target - progress);
            if (progress >= target) {
                progress = 0;
                server.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
                List<ItemStack> drops = BlockUtil.breakBlockAndGetDrops(server, currentPos);
                if (drops != null) {
                    drops.forEach(stack -> InventoryUtil.addToBestAcceptor(server, worldPosition, stack));
                }
                nextPos();
            } else {
                server.destroyBlockProgress(currentPos.hashCode(), currentPos, (int) (progress * 9 / target));
            }
        } else if (currentPos != null || ++idleTicks >= RECHECK_INTERVAL) {
            idleTicks = 0;
            nextPos();
        }
    }

    @Override
    protected @Nullable BlockPos getTargetPos() {
        return currentPos != null ? currentPos : stopPos;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (stopPos != null) {
            output.putLong("stopPos", stopPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stopPos = input.getLong("stopPos").map(BlockPos::of).orElse(null);
    }

    private boolean canBreak(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || BlockUtil.isUnbreakable(level, pos) || state.is(BCFactoryBlocks.TUBE.get())) {
            return false;
        }
        // Liquids can't be mined, but blocks with a fluid in them (such as waterlogged ones) can
        FluidState fluid = level.getFluidState(pos);
        return fluid.isEmpty() || !state.liquid();
    }

    private void nextPos() {
        if (level == null) return;
        BlockPos pos = worldPosition;
        while (true) {
            pos = pos.below();
            if (level.isOutsideBuildHeight(pos) || worldPosition.getY() - pos.getY() > BlockUtil.MINING_MAX_DEPTH) {
                break;
            }
            if (canBreak(pos)) {
                currentPos = pos;
                stopPos = null;
                updateLength();
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(BCFactoryBlocks.TUBE.get()) && !state.liquid()) {
                stopPos = pos;
                break;
            }
        }
        if (currentPos != null) {
            level.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
        }
        currentPos = null;
        updateLength();
    }
}
