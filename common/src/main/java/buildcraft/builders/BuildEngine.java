/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.InventoryUtil;

/** Builds a plan in an area of the world, block by block, using power and the blocks in an inventory: breaking the blocks
 * that are in the way (if allowed) and placing the ones that are missing. Used by the filler and the builder.
 * <p>
 * The area is checked a part at a time, and only once all of it has been checked does building start. Blocks are broken
 * from the top down, and placed from the bottom up. */
public final class BuildEngine {
    private static final long MAX_POWER_PER_TICK = 256 * MjAPI.MJ;
    private static final int MAX_SCAN_PER_TICK = 4096;
    private static final int MAX_TASKS_PER_TICK = 4;
    private static final byte UNKNOWN = 0, CORRECT = 1, TO_BREAK = 2, TO_PLACE = 3;

    /** What should be built. */
    public interface Plan {
        /** @return The state that should be at a position in the area (by index, see {@link BuildEngine#posOf}): air for
         *         nothing, null for any block, or a specific state. */
        @Nullable
        BlockState getWanted(int index);

        /** @return True if the position doesn't matter (for example, the top half of a door, which is placed with the
         *         bottom half). */
        default boolean isSkipped(int index) {
            return false;
        }
    }

    private final ServerLevel level;
    private final BlockPos ownerPos;
    private final BoundingBox box;
    private final Plan plan;
    private final int sizeX, sizeZ;
    private final byte[] checks;
    private final int[] breakOrder, placeOrder;
    private int scanIndex = 0;
    private boolean fullScanDone = false;
    private int breakCursor = 0, placeCursor = 0;
    private int leftToBreak = 0, leftToPlace = 0;
    private int taskIndex = -1;
    private boolean taskIsBreak;
    private long taskPower;

    public BuildEngine(ServerLevel level, BlockPos ownerPos, BoundingBox box, Plan plan) {
        this.level = level;
        this.ownerPos = ownerPos;
        this.box = box;
        this.plan = plan;
        this.sizeX = box.getXSpan();
        this.sizeZ = box.getZSpan();
        int count = sizeX * box.getYSpan() * sizeZ;
        checks = new byte[count];
        double cx = sizeX / 2.0, cz = sizeZ / 2.0;
        Integer[] sorted = new Integer[count];
        for (int i = 0; i < count; i++) sorted[i] = i;
        Comparator<Integer> horizontal = Comparator.comparingDouble(i -> {
            double dx = i % sizeX + 0.5 - cx, dz = (i / sizeX) % sizeZ + 0.5 - cz;
            return dx * dx + dz * dz;
        });
        Comparator<Integer> byY = Comparator.comparingInt(i -> i / (sizeX * sizeZ));
        Arrays.sort(sorted, byY.reversed().thenComparing(horizontal));
        breakOrder = Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
        Arrays.sort(sorted, byY.thenComparing(horizontal));
        placeOrder = Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
    }

    /** @return The position of an index: x first, then z, then y, from the lowest corner of the box. */
    public BlockPos posOf(int index) {
        int x = index % sizeX, z = (index / sizeX) % sizeZ, y = index / (sizeX * sizeZ);
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    public BoundingBox getBox() {
        return box;
    }

    public int getLeftToBreak() {
        return leftToBreak;
    }

    public int getLeftToPlace() {
        return leftToPlace;
    }

    public boolean isFinished(boolean canExcavate) {
        return fullScanDone && leftToPlace == 0 && (leftToBreak == 0 || !canExcavate);
    }

    /** Call when the inventory changes, so that positions skipped for want of items are tried again. */
    public void onResourcesChanged() {
        placeCursor = 0;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || (!state.getFluidState().isEmpty() && state.canBeReplaced());
    }

    private byte check(int index) {
        BlockPos pos = posOf(index);
        if (pos.equals(ownerPos) || !level.isLoaded(pos) || plan.isSkipped(index)) return CORRECT;
        BlockState state = level.getBlockState(pos);
        BlockState wanted = plan.getWanted(index);
        boolean unbreakable = state.getDestroySpeed(level, pos) < 0;
        if (wanted == null) {
            return state.canBeReplaced() ? TO_PLACE : CORRECT;
        }
        if (wanted.isAir()) {
            return isEmpty(state) || unbreakable ? CORRECT : TO_BREAK;
        }
        if (state.is(wanted.getBlock())) return CORRECT;
        if (state.canBeReplaced()) return TO_PLACE;
        return unbreakable ? CORRECT : TO_BREAK;
    }

    private void setCheck(int index, byte result) {
        byte old = checks[index];
        if (old == result) return;
        if (old == TO_BREAK) leftToBreak--;
        if (old == TO_PLACE) leftToPlace--;
        if (result == TO_BREAK) leftToBreak++;
        if (result == TO_PLACE) leftToPlace++;
        checks[index] = result;
    }

    /** Checks part of the area, then (if allowed to work) builds what it can with the power available. */
    public void tick(MjBattery battery, Container resources, boolean canExcavate, boolean working) {
        int count = checks.length;
        for (int n = 0; n < Math.min(MAX_SCAN_PER_TICK, count); n++) {
            setCheck(scanIndex, check(scanIndex));
            scanIndex++;
            if (scanIndex >= count) {
                scanIndex = 0;
                fullScanDone = true;
                breakCursor = placeCursor = 0;
            }
        }
        if (!working || !fullScanDone) return;
        long powerLeft = MAX_POWER_PER_TICK;
        for (int t = 0; t < MAX_TASKS_PER_TICK && powerLeft > 0; t++) {
            if (taskIndex < 0 && !pickTask(resources, canExcavate)) break;
            BlockPos pos = posOf(taskIndex);
            long target = taskIsBreak ? BlockUtil.computeBlockBreakPower(level, pos)
                : (long) (Math.sqrt(pos.distSqr(ownerPos)) * 10 * MjAPI.MJ);
            long got = battery.extractPower(0, Math.min(target - taskPower, powerLeft));
            taskPower += got;
            powerLeft -= got;
            if (taskPower < target) break;
            doTask(pos, resources);
            taskIndex = -1;
            taskPower = 0;
        }
    }

    private boolean pickTask(Container resources, boolean canExcavate) {
        if (canExcavate && leftToBreak > 0) {
            for (; breakCursor < breakOrder.length; breakCursor++) {
                int index = breakOrder[breakCursor];
                if (checks[index] == TO_BREAK) {
                    startTask(index, true);
                    return true;
                }
            }
        }
        // Nothing is placed while there are still blocks to break
        if (leftToPlace > 0 && (!canExcavate || leftToBreak == 0)) {
            for (; placeCursor < placeOrder.length; placeCursor++) {
                int index = placeOrder[placeCursor];
                if (checks[index] == TO_PLACE && findItem(resources, plan.getWanted(index)) >= 0) {
                    startTask(index, false);
                    return true;
                }
            }
        }
        return false;
    }

    private void startTask(int index, boolean isBreak) {
        taskIndex = index;
        taskIsBreak = isBreak;
        taskPower = 0;
    }

    /** @return The slot with an item that can place the wanted state (any block, if null), or -1. */
    private static int findItem(Container resources, @Nullable BlockState wanted) {
        Item needed = wanted == null ? null : wanted.getBlock().asItem();
        for (int i = 0; i < resources.getContainerSize(); i++) {
            ItemStack stack = resources.getItem(i);
            if (needed == null ? stack.getItem() instanceof BlockItem : stack.is(needed)) return i;
        }
        return -1;
    }

    private void doTask(BlockPos pos, Container resources) {
        byte now = check(taskIndex);
        setCheck(taskIndex, now);
        if (taskIsBreak) {
            if (now != TO_BREAK) return;
            List<ItemStack> drops = BlockUtil.breakBlockAndGetDrops(level, pos);
            if (drops != null) {
                for (ItemStack drop : drops) {
                    InventoryUtil.addToBestAcceptor(level, ownerPos, drop);
                }
            }
        } else {
            if (now != TO_PLACE) return;
            BlockState wanted = plan.getWanted(taskIndex);
            int slot = findItem(resources, wanted);
            if (slot < 0) return;
            ItemStack stack = resources.getItem(slot);
            boolean placed;
            if (wanted == null) {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                InteractionResult result = ((BlockItem) stack.getItem())
                    .place(new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack, hit));
                placed = result.consumesAction();
            } else {
                ItemStack used = stack.copyWithCount(1);
                placed = level.setBlock(pos, wanted, Block.UPDATE_ALL);
                if (placed) {
                    stack.shrink(1);
                    // Places the other half of doors, beds and tall plants
                    wanted.getBlock().setPlacedBy(level, pos, wanted, null, used);
                }
            }
            if (placed) {
                resources.setChanged();
            } else {
                // Can't be placed here (for example, an entity is in the way): skip it until the next pass
                placeCursor++;
            }
        }
        setCheck(taskIndex, check(taskIndex));
    }
}
