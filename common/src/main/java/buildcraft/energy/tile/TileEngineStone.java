/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.container.ContainerEngineStone;
import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.FuelUtil;

/** The stirling engine: burns furnace fuel to make power. */
public class TileEngineStone extends TileEngineBase implements WorldlyContainer, MenuProvider {
    private static final long MAX_OUTPUT = MjAPI.MJ;
    private static final long MIN_OUTPUT = MAX_OUTPUT / 3;
    private static final long E_LIMIT = (MAX_OUTPUT - MIN_OUTPUT) * 20;
    private static final int[] SLOTS = { 0 };

    public final ItemHandlerSimple invFuel = new ItemHandlerSimple(1, (slot, stack) -> isValidFuel(stack), this::setChanged);

    public int burnTime = 0;
    public int totalBurnTime = 0;
    private long esum = 0;
    /** Set while the slot holds a leftover container item (such as an empty bucket) that isn't fuel. */
    private boolean isForceInserting = false;

    public TileEngineStone(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_STIRLING_TILE.get(), pos, state);
    }

    private boolean isValidFuel(ItemStack stack) {
        if (isForceInserting) return true;
        // The client can't work out burn times, so let it accept anything the server would.
        return level == null || level.isClientSide() || getItemBurnTime(stack) > 0;
    }

    private int getItemBurnTime(ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        return FuelUtil.getBurnTime(serverLevel, this, stack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("burnTime", 0);
        totalBurnTime = input.getIntOr("totalBurnTime", 0);
        esum = input.getLongOr("esum", 0);
        isForceInserting = input.getBooleanOr("isForceInserting", false);
        invFuel.load(input, "fuel");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("totalBurnTime", totalBurnTime);
        output.putLong("esum", esum);
        output.putBoolean("isForceInserting", isForceInserting);
        invFuel.save(output, "fuel");
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    protected void engineUpdate() {
        super.engineUpdate();
        if (burnTime > 0) {
            burnTime--;
            if (getPowerStage() != EnumPowerStage.OVERHEAT) {
                long output = getCurrentOutput();
                currentOutput = output;
                addPower(output);
            }
        }
    }

    @Override
    public void burn() {
        if (burnTime == 0 && isRedstonePowered) {
            ItemStack fuelStack = invFuel.getItem(0);
            burnTime = totalBurnTime = isForceInserting ? 0 : getItemBurnTime(fuelStack);
            if (burnTime > 0) {
                ItemStack fuel = invFuel.removeItem(0, 1);
                var remainder = fuel.getItem().getCraftingRemainder();
                ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
                if (!container.isEmpty()) {
                    if (invFuel.getItem(0).isEmpty()) {
                        isForceInserting = true;
                        invFuel.setItem(0, container);
                    } else if (level != null) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5, container);
                    }
                }
            }
        }
        if (isForceInserting && invFuel.getItem(0).isEmpty()) {
            isForceInserting = false;
        }
    }

    @Override
    public long maxPowerReceived() {
        return 200 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 100 * MjAPI.MJ;
    }

    @Override
    public long getMaxPower() {
        return 1000 * MjAPI.MJ;
    }

    @Override
    public float explosionRange() {
        return 2;
    }

    @Override
    public long getCurrentOutput() {
        long e = 3 * getMaxPower() / 8 - power;
        esum = clamp(esum + e, -E_LIMIT, E_LIMIT);
        return clamp(e + esum / 20, MIN_OUTPUT, MAX_OUTPUT);
    }

    private static long clamp(long val, long min, long max) {
        return Math.max(min, Math.min(max, val));
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerEngineStone(id, inventory, this);
    }

    // WorldlyContainer, so hoppers and other mods can add fuel

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return invFuel.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // Only let empty containers (such as buckets) be taken out
        return isForceInserting;
    }

    @Override
    public int getContainerSize() {
        return invFuel.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return invFuel.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return invFuel.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return invFuel.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return invFuel.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        invFuel.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return invFuel.stillValid(player);
    }

    @Override
    public void clearContent() {
        invFuel.clearContent();
    }
}
