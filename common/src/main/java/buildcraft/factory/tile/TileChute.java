/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.container.ContainerChute;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.tile.TileBC;

/** Picks up items in front of it (for free if it faces up, otherwise with MJ) and pushes them into the inventories
 * around it. */
public class TileChute extends TileBC implements WorldlyContainer, MenuProvider, IMjConnectorProvider {
    private static final int PICKUP_MAX = 3;
    private static final long PICKUP_POWER = MjAPI.MJ / 10;
    private static final int[] SLOTS = { 0, 1, 2, 3 };

    public final ItemHandlerSimple inv = new ItemHandlerSimple(4, this::setChanged);
    private final MjBattery battery = new MjBattery(MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);
    private long progress = 0;

    public TileChute(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.CHUTE_TILE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockChute.FACING)) return;
        Direction facing = state.getValue(BlockChute.FACING);

        battery.tick();
        if (facing == Direction.UP) {
            progress += PICKUP_POWER / 100; // Gravity does some of the work
        }
        progress += battery.extractPower(0, PICKUP_POWER - progress);
        if (progress >= PICKUP_POWER) {
            progress = 0;
            pickupItems(facing);
        }
        putInNearInventories(facing);
    }

    private void pickupItems(Direction facing) {
        if (level == null) return;
        AABB box = extrudeFace(facing, 0.25);
        int count = PICKUP_MAX;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            ItemStack toInsert = stack.copyWithCount(Math.min(count, stack.getCount()));
            ItemStack left = insert(toInsert, false);
            int moved = toInsert.getCount() - left.getCount();
            if (moved > 0) {
                ItemStack remaining = stack.copy();
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(remaining);
                }
                count -= moved;
            }
            if (count <= 0) return;
        }
    }

    private AABB extrudeFace(Direction face, double depth) {
        AABB block = new AABB(worldPosition);
        return switch (face) {
            case UP -> new AABB(block.minX, block.maxY, block.minZ, block.maxX, block.maxY + depth, block.maxZ);
            case DOWN -> new AABB(block.minX, block.minY - depth, block.minZ, block.maxX, block.minY, block.maxZ);
            case NORTH -> new AABB(block.minX, block.minY, block.minZ - depth, block.maxX, block.maxY, block.minZ);
            case SOUTH -> new AABB(block.minX, block.minY, block.maxZ, block.maxX, block.maxY, block.maxZ + depth);
            case WEST -> new AABB(block.minX - depth, block.minY, block.minZ, block.minX, block.maxY, block.maxZ);
            case EAST -> new AABB(block.maxX, block.minY, block.minZ, block.maxX + depth, block.maxY, block.maxZ);
        };
    }

    /** Inserts into the chute's inventory, filling existing stacks first. */
    private ItemStack insert(ItemStack stack, boolean simulate) {
        ItemStack left = stack.copy();
        for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
            for (int slot = 0; slot < inv.getContainerSize() && !left.isEmpty(); slot++) {
                ItemStack existing = inv.getItem(slot);
                if (pass == 0 ? existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, left)
                    : !existing.isEmpty()) {
                    continue;
                }
                int limit = Math.min(left.getMaxStackSize(), inv.getMaxStackSize());
                int toMove = Math.min(limit - existing.getCount(), left.getCount());
                if (toMove <= 0) continue;
                if (!simulate) {
                    inv.setItem(slot, left.copyWithCount(existing.getCount() + toMove));
                }
                left.shrink(toMove);
            }
        }
        return left;
    }

    private void putInNearInventories(Direction facing) {
        if (level == null || inv.isEmpty()) return;
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.remove(facing);
        Collections.shuffle(sides);
        for (Direction side : sides) {
            if (level.getBlockEntity(worldPosition.relative(side)) == null) continue;
            IItemTransactor transactor = Platform.INSTANCE.getItemTransactor(level, worldPosition.relative(side), side.getOpposite());
            if (transactor == null) continue;
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty()) continue;
                if (transactor.insert(stack.copyWithCount(1), false).isEmpty()) {
                    inv.removeItem(slot, 1);
                    break;
                }
            }
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!player.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inv.save(output, "inv");
        battery.save(output, "battery");
        output.putLong("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inv.load(input, "inv");
        battery.load(input, "battery");
        progress = input.getLongOr("progress", 0);
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerChute(id, inventory, this);
    }

    // WorldlyContainer: other blocks can insert, but not extract

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return inv.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return inv.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inv.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inv.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.stillValid(player);
    }

    @Override
    public void clearContent() {
        inv.clearContent();
    }
}
