/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.MjAPI;
import buildcraft.lib.crafting.WorkbenchCrafting;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;

/** An automatic crafting table powered by lasers, with room for more materials and results. */
public class TileAdvancedCraftingTable extends TileLaserTableBase implements WorldlyContainer {
    public static final long POWER_REQ = 500 * MjAPI.MJ;
    private static final int MATERIALS = 15, RESULTS = 9;
    private static final int[] ALL_SLOTS = new int[MATERIALS + RESULTS];

    static {
        for (int i = 0; i < ALL_SLOTS.length; i++) ALL_SLOTS[i] = i;
    }

    public final ItemHandlerSimple invBlueprint = new ItemHandlerSimple(9, this::onBlueprintChanged);
    public final ItemHandlerSimple invMaterials = new ItemHandlerSimple(MATERIALS, (slot, stack) -> isMaterial(stack), this::setChanged);
    public final ItemHandlerSimple invResults = new ItemHandlerSimple(RESULTS, (slot, stack) -> false, this::setChanged);
    private final WorkbenchCrafting crafting = new WorkbenchCrafting(invBlueprint, invMaterials, invResults);
    public final SimpleContainer preview = crafting.preview;
    private boolean canCraft = false;

    public TileAdvancedCraftingTable(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_TILE.get(), pos, state);
    }

    private void onBlueprintChanged() {
        crafting.onBlueprintChanged();
        setChanged();
    }

    private boolean isMaterial(ItemStack stack) {
        return crafting.isMaterial(stack);
    }

    @Override
    public long getTarget() {
        return canCraft ? POWER_REQ : 0;
    }

    @Override
    public void tick() {
        if (level instanceof ServerLevel server) {
            crafting.tick(server);
            canCraft = crafting.canCraft();
            if (canCraft && power >= POWER_REQ && crafting.craft(server, worldPosition)) {
                power -= POWER_REQ;
            }
        }
        super.tick();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invBlueprint.save(output, "blueprint");
        invMaterials.save(output, "materials");
        invResults.save(output, "results");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invBlueprint.load(input, "blueprint");
        invMaterials.load(input, "materials");
        invResults.load(input, "results");
        crafting.markDirty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerAdvancedCraftingTable(id, inventory, this);
    }

    // WorldlyContainer: materials in, results out

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < MATERIALS && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= MATERIALS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < MATERIALS && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public int getContainerSize() {
        return ALL_SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        return invMaterials.isEmpty() && invResults.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < MATERIALS ? invMaterials.getItem(slot) : invResults.getItem(slot - MATERIALS);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return slot < MATERIALS ? invMaterials.removeItem(slot, count) : invResults.removeItem(slot - MATERIALS, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot < MATERIALS ? invMaterials.removeItemNoUpdate(slot) : invResults.removeItemNoUpdate(slot - MATERIALS);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < MATERIALS) {
            invMaterials.setItem(slot, stack);
        } else {
            invResults.setItem(slot - MATERIALS, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return invMaterials.stillValid(player);
    }

    @Override
    public void clearContent() {
        invMaterials.clearContent();
        invResults.clearContent();
    }
}
