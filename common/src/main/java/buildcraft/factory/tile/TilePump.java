/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryConfig;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.misc.BlockUtil;

/** Pumps up fluid from below, starting with the source blocks furthest away. */
public class TilePump extends TileMiner implements IFluidHandlerProvider {
    private static final Direction[] SEARCH_NORMAL = { Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    private static final Direction[] SEARCH_GASEOUS = { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    private static final int REBUILD_DELAY = 30;
    private static final long POWER_PER_BUCKET = 10 * MjAPI.MJ;

    /** A path back through the fluid from a block to the pump, so we can check the block is still connected. */
    private record FluidPath(BlockPos pos, @Nullable FluidPath parent) {}

    private final Tank tank = new Tank("tank", 16 * BCFluidStack.BUCKET, this::onTankChanged);
    private boolean queueBuilt = false;
    private final Map<BlockPos, FluidPath> paths = new HashMap<>();
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private boolean isInfiniteWaterSource;
    private int rebuildTimer = 0;
    /** The position just below the bottom of the pump tube. */
    @Nullable
    private BlockPos targetPos;

    public TilePump(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.PUMP_TILE.get(), pos, state);
        tank.canFill = false;
    }

    private void onTankChanged() {
        setChanged();
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjBatteryReceiver.Redstone(battery);
    }

    @Override
    protected long getBatteryCapacity() {
        return 50 * MjAPI.MJ;
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return tank;
    }

    public Tank getTank() {
        return tank;
    }

    private void buildQueue() {
        if (level == null) return;
        queue.clear();
        paths.clear();
        isInfiniteWaterSource = false;
        Fluid queueFluid = Fluids.EMPTY;
        Set<BlockPos> checked = new HashSet<>();
        List<BlockPos> toCheck = new ArrayList<>();
        for (targetPos = worldPosition.below(); !level.isOutsideBuildHeight(targetPos); targetPos = targetPos.below()) {
            if (worldPosition.getY() - targetPos.getY() > BlockUtil.MINING_MAX_DEPTH) {
                break;
            }
            Fluid fluid = BlockUtil.getFluidWithFlowing(level, targetPos);
            if (fluid != Fluids.EMPTY) {
                queueFluid = fluid;
                toCheck.add(targetPos);
                paths.put(targetPos, new FluidPath(targetPos, null));
                checked.add(targetPos);
                if (BlockUtil.getFluid(level, targetPos) != Fluids.EMPTY) {
                    queue.add(targetPos);
                }
                break;
            }
            BlockState state = level.getBlockState(targetPos);
            if (!state.isAir() && !state.is(BCFactoryBlocks.TUBE.get())) {
                break;
            }
        }
        if (toCheck.isEmpty()) {
            return;
        }

        Direction[] directions = isGaseous(queueFluid) ? SEARCH_GASEOUS : SEARCH_NORMAL;
        boolean isWater = !BCFactoryConfig.pumpsConsumeWater && queueFluid.defaultFluidState().is(FluidTags.WATER);
        int maxDistanceSq = BlockUtil.PUMP_MAX_DISTANCE * BlockUtil.PUMP_MAX_DISTANCE;
        outer:
        while (!toCheck.isEmpty()) {
            List<BlockPos> current = new ArrayList<>(toCheck);
            toCheck.clear();
            for (BlockPos pos : current) {
                int count = 0;
                for (Direction side : directions) {
                    BlockPos offset = pos.relative(side);
                    if (offset.distSqr(targetPos) > maxDistanceSq) {
                        continue;
                    }
                    if (checked.add(offset)) {
                        if (BlockUtil.getFluidWithFlowing(level, offset) == queueFluid) {
                            paths.put(offset, new FluidPath(offset, paths.get(pos)));
                            if (BlockUtil.getFluid(level, offset) != Fluids.EMPTY) {
                                queue.add(offset);
                            }
                            toCheck.add(offset);
                            count++;
                        }
                    } else {
                        // Already checked, so it must be part of the same pool
                        count++;
                    }
                }
                if (isWater && count >= 2) {
                    // The same check that water uses to create new source blocks
                    BlockState below = level.getBlockState(pos.below());
                    if (level.getFluidState(pos.below()).is(FluidTags.WATER) && level.getFluidState(pos.below()).isSource()
                        || below.isSolid()) {
                        isInfiniteWaterSource = true;
                        break outer;
                    }
                }
            }
        }
    }

    private static boolean isGaseous(Fluid fluid) {
        return fluid instanceof BCFluid bc && bc.def.isGaseous();
    }

    private boolean canDrain(BlockPos pos) {
        if (level == null) return false;
        Fluid fluid = BlockUtil.getFluid(level, pos);
        if (fluid == Fluids.EMPTY) return false;
        return tank.isEmpty() || tank.getFluid().isSameFluid(fluid);
    }

    private void nextPos() {
        while (!queue.isEmpty()) {
            currentPos = queue.removeLast();
            if (canDrain(currentPos)) {
                updateLength();
                return;
            }
        }
        currentPos = null;
        updateLength();
    }

    @Override
    protected @Nullable BlockPos getTargetPos() {
        return queue.isEmpty() && currentPos == null ? null : targetPos;
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            if (!queueBuilt) {
                buildQueue();
                queueBuilt = true;
            }
            super.tick();
            FluidUtilBC.pushFluidAround(level, worldPosition, tank);
        } else {
            super.tick();
        }
    }

    @Override
    protected void mine() {
        if (level == null || tank.getFluidAmount() > tank.getCapacity() / 2) {
            return;
        }
        if (currentPos != null && paths.containsKey(currentPos)) {
            progress += (int) battery.extractPower(0, POWER_PER_BUCKET - progress);
            if (progress < POWER_PER_BUCKET) {
                return;
            }
            BCFluidStack drain = BlockUtil.drainBlock(level, currentPos, true);
            if (!drain.isEmpty() && getFirstInvalidPointOnPath(currentPos) == null && canDrain(currentPos)) {
                tank.fillInternal(drain, false);
                progress = 0;
                if (isInfiniteWaterSource) {
                    isInfiniteWaterSource = drain.getFluid().defaultFluidState().is(FluidTags.WATER);
                }
                if (!isInfiniteWaterSource) {
                    BlockUtil.drainBlock(level, currentPos, false);
                    paths.remove(currentPos);
                    nextPos();
                }
                return;
            }
            if (++rebuildTimer < REBUILD_DELAY) {
                return;
            }
        } else if (currentPos == null && ++rebuildTimer < REBUILD_DELAY) {
            return;
        }
        rebuildTimer = 0;
        buildQueue();
        nextPos();
    }

    @Nullable
    private BlockPos getFirstInvalidPointOnPath(BlockPos from) {
        FluidPath path = paths.get(from);
        if (path == null) {
            return from;
        }
        do {
            if (BlockUtil.getFluidWithFlowing(level, path.pos()) == Fluids.EMPTY) {
                return path.pos();
            }
        } while ((path = path.parent()) != null);
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input);
    }
}
