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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.tile.TileBC;

/** Places source blocks of the fluid it is given around itself, filling the nearest spaces first. */
public class TileFloodGate extends TileBC implements IFluidHandlerProvider {
    private static final Direction[] SEARCH_NORMAL = { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    private static final Direction[] SEARCH_GASEOUS = { Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    private static final int[] REBUILD_DELAYS = { 16, 32, 64, 128, 256 };
    private static final int MAX_QUEUE = 4096;

    private final Tank tank = new Tank("tank", 2 * BCFluidStack.BUCKET, this::setChanged);
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private final Map<BlockPos, List<BlockPos>> paths = new HashMap<>();
    private int delayIndex = 0;
    private int tick = 0;

    public TileFloodGate(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.FLOOD_GATE_TILE.get(), pos, state);
    }

    public void onSidesChanged() {
        queue.clear();
        delayIndex = 0;
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return tank;
    }

    private Fluid getTankFluid() {
        return tank.getFluid().getFluid();
    }

    private void buildQueue() {
        if (level == null) return;
        queue.clear();
        paths.clear();
        if (tank.isEmpty()) return;
        Set<BlockPos> checked = new HashSet<>();
        checked.add(worldPosition);
        List<BlockPos> toCheck = new ArrayList<>();
        BlockState state = getBlockState();
        for (Direction side : Direction.values()) {
            if (BlockFloodGate.isOpen(state, side)) {
                BlockPos offset = worldPosition.relative(side);
                toCheck.add(offset);
                paths.put(offset, List.of(offset));
            }
        }
        Fluid fluid = getTankFluid();
        Direction[] directions = fluid instanceof BCFluid bc && bc.def.isGaseous() ? SEARCH_GASEOUS : SEARCH_NORMAL;
        outer:
        while (!toCheck.isEmpty()) {
            List<BlockPos> current = new ArrayList<>(toCheck);
            toCheck.clear();
            for (BlockPos pos : current) {
                if (pos.distSqr(worldPosition) > 64 * 64 || !checked.add(pos) || !canSearch(pos)) {
                    continue;
                }
                if (canFill(pos)) {
                    queue.push(pos);
                    if (queue.size() >= MAX_QUEUE) {
                        break outer;
                    }
                }
                List<BlockPos> path = paths.get(pos);
                for (Direction side : directions) {
                    BlockPos next = pos.relative(side);
                    if (checked.contains(next)) continue;
                    List<BlockPos> nextPath = new ArrayList<>(path.size() + 1);
                    nextPath.addAll(path);
                    nextPath.add(next);
                    paths.put(next, nextPath);
                    toCheck.add(next);
                }
            }
        }
    }

    /** @return True if a source block can be placed here: it is air, flowing fluid of the right kind, or something
     *         fluid can wash away. */
    private boolean canFill(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        Fluid fluid = BlockUtil.getFluidWithFlowing(level, pos);
        if (fluid != Fluids.EMPTY) {
            return fluid == getTankFluid() && !level.getFluidState(pos).isSource();
        }
        return state.canBeReplaced(getTankFluid()) && !(state.getBlock() instanceof LiquidBlockContainer);
    }

    private boolean canSearch(BlockPos pos) {
        if (canFill(pos)) return true;
        return level != null && BlockUtil.getFluid(level, pos) == getTankFluid();
    }

    private boolean canFillThrough(BlockPos pos) {
        return level != null && BlockUtil.getFluidWithFlowing(level, pos) == getTankFluid();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (tank.getFluidAmount() < BCFluidStack.BUCKET) return;

        tick++;
        if (tick % 16 == 0 && !queue.isEmpty()) {
            BlockPos current = queue.removeLast();
            List<BlockPos> path = paths.get(current);
            boolean canFill = true;
            if (path != null) {
                for (BlockPos p : path) {
                    if (!p.equals(current) && !canFillThrough(p)) {
                        canFill = false;
                        break;
                    }
                }
            }
            if (canFill && canFill(current)) {
                Fluid fluid = getTankFluid();
                BlockState fluidState = fluid instanceof FlowingFluid flowing
                    ? flowing.getSource(false).createLegacyBlock() : fluid.defaultFluidState().createLegacyBlock();
                if (!fluidState.isAir()) {
                    BlockState old = level.getBlockState(current);
                    if (!old.isAir() && !old.liquid()) {
                        level.destroyBlock(current, true);
                    }
                    level.setBlock(current, fluidState, Block.UPDATE_ALL);
                    tank.drainInternal(BCFluidStack.BUCKET, false);
                    delayIndex = 0;
                    tick = 0;
                }
            } else {
                buildQueue();
            }
        }

        if (queue.isEmpty() && tick >= REBUILD_DELAYS[delayIndex]) {
            delayIndex = Math.min(delayIndex + 1, REBUILD_DELAYS.length - 1);
            tick = 0;
            buildQueue();
        }
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

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) buildcraft.core.item.ItemFragileFluidShard.dropFluids(level, pos, tank);
    }
}
