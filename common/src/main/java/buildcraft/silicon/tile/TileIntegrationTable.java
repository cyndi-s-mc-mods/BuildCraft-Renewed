/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.recipes.IntegrationRecipes;
import buildcraft.api.recipes.IntegrationRecipes.IntegrationRecipe;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.container.ContainerIntegrationTable;

/** Combines the item in its centre slot with the items around it, using power from lasers (see
 * {@link IntegrationRecipes}). */
public class TileIntegrationTable extends TileLaserTableBase implements WorldlyContainer {
    public static final int AROUND = 8;
    private static final int[] SLOTS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

    public final ItemHandlerSimple invTarget = new ItemHandlerSimple(1, this::setChanged);
    public final ItemHandlerSimple invToIntegrate = new ItemHandlerSimple(AROUND, this::setChanged);
    public final ItemHandlerSimple invResult = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);
    /** Shows what the current recipe makes. */
    public final SimpleContainer preview = new SimpleContainer(1);

    @Nullable
    private IntegrationRecipe recipe;

    public TileIntegrationTable(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.INTEGRATION_TABLE_TILE.get(), pos, state);
    }

    private List<ItemStack> around() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < AROUND; i++) {
            list.add(invToIntegrate.getItem(i));
        }
        return list;
    }

    public ItemStack getOutput() {
        return recipe == null ? ItemStack.EMPTY : recipe.getOutput(invTarget.getItem(0), around());
    }

    private boolean hasSpaceFor(ItemStack stack) {
        ItemStack result = invResult.getItem(0);
        return result.isEmpty()
            || ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() + result.getCount() <= result.getMaxStackSize();
    }

    @Override
    public long getTarget() {
        ItemStack output = getOutput();
        return recipe != null && !output.isEmpty() && hasSpaceFor(output) ? recipe.getPower(output) : 0;
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            if (recipe == null || getOutput().isEmpty()) {
                recipe = IntegrationRecipes.find(invTarget.getItem(0), around());
            }
            ItemStack output = getOutput();
            preview.setItem(0, output);
            long target = getTarget();
            if (target > 0 && power >= target && recipe != null) {
                if (extract(invToIntegrate, recipe.getRequirements(output), true)) {
                    extract(invToIntegrate, recipe.getRequirements(output), false);
                    invTarget.removeItem(0, recipe.getTarget().count());
                    ItemStack result = invResult.getItem(0);
                    invResult.setItem(0, result.isEmpty() ? output : result.copyWithCount(result.getCount() + output.getCount()));
                    power -= target;
                }
            }
        }
        super.tick();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invTarget.save(output, "target");
        invToIntegrate.save(output, "toIntegrate");
        invResult.save(output, "result");
        if (recipe != null) output.putString("recipe", recipe.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invTarget.load(input, "target");
        invToIntegrate.load(input, "toIntegrate");
        invResult.load(input, "result");
        recipe = input.getString("recipe").map(IntegrationRecipes::get).orElse(null);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerIntegrationTable(id, inventory, this);
    }

    // WorldlyContainer: pipes insert the centre item from above and the others from the sides, and take results from below

    private ItemHandlerSimple invFor(int slot) {
        return slot == 0 ? invTarget : slot <= AROUND ? invToIntegrate : invResult;
    }

    private int indexFor(int slot) {
        return slot == 0 ? 0 : slot <= AROUND ? slot - 1 : 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return side == Direction.UP;
        return slot <= AROUND && side != Direction.UP && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == AROUND + 1;
    }

    @Override
    public int getContainerSize() {
        return AROUND + 2;
    }

    @Override
    public boolean isEmpty() {
        return invTarget.isEmpty() && invToIntegrate.isEmpty() && invResult.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return invFor(slot).getItem(indexFor(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return invFor(slot).removeItem(indexFor(slot), count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return invFor(slot).removeItemNoUpdate(indexFor(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        invFor(slot).setItem(indexFor(slot), stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return invTarget.stillValid(player);
    }

    @Override
    public void clearContent() {
        invTarget.clearContent();
        invToIntegrate.clearContent();
        invResult.clearContent();
    }
}
