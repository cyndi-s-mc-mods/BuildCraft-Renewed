/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerAutoWorkbench;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.tile.TileBC;

/** Crafts the recipe laid out in its blueprint grid, using the materials put into it. It works slowly by itself and
 * much faster with power. */
public class TileAutoWorkbench extends TileBC implements WorldlyContainer, MenuProvider, IMjConnectorProvider {
    /** A redstone engine makes {@code 1 MJ} per tick, so the workbench is a lot slower without one. */
    private static final long POWER_GEN_PASSIVE = MjAPI.MJ / 5;
    /** It takes 10 seconds to craft an item without power. */
    public static final long POWER_REQUIRED = POWER_GEN_PASSIVE * 20 * 10;
    private static final long POWER_LOST = POWER_GEN_PASSIVE * 10;
    public static final int RESULT_SLOT = 9;
    private static final int[] SLOTS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, RESULT_SLOT };

    public final ItemHandlerSimple invBlueprint = new ItemHandlerSimple(9, this::onBlueprintChanged);
    public final ItemHandlerSimple invMaterials = new ItemHandlerSimple(9, (slot, stack) -> isMaterial(stack), this::setChanged);
    public final ItemHandlerSimple invResult = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);
    /** What the blueprint makes, shown in the GUI. */
    public final SimpleContainer preview = new SimpleContainer(1);

    private long powerStored = 0;
    private boolean recipeDirty = true;
    @Nullable
    private RecipeHolder<CraftingRecipe> recipe;
    private final IMjRedstoneReceiver receiver = new Receiver();

    public TileAutoWorkbench(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.AUTO_WORKBENCH_TILE.get(), pos, state);
    }

    private void onBlueprintChanged() {
        for (int i = 0; i < invBlueprint.getContainerSize(); i++) {
            ItemStack stack = invBlueprint.getItem(i);
            if (stack.getCount() > 1) {
                invBlueprint.getItems().set(i, stack.copyWithCount(1));
            }
        }
        recipeDirty = true;
        setChanged();
    }

    private boolean isMaterial(ItemStack stack) {
        for (ItemStack bpt : invBlueprint.getItems()) {
            if (!bpt.isEmpty() && ItemStack.isSameItemSameComponents(bpt, stack)) return true;
        }
        return false;
    }

    public long getPowerStored() {
        return powerStored;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        if (recipeDirty) {
            recipeDirty = false;
            updateRecipe(server);
        }
        if (canCraft()) {
            if (powerStored >= POWER_REQUIRED) {
                craft(server);
                powerStored = canCraft() ? 1 : 0;
            } else {
                powerStored += POWER_GEN_PASSIVE;
            }
        } else if (powerStored >= POWER_LOST) {
            powerStored -= POWER_LOST;
        } else {
            powerStored = 0;
        }
    }

    private CraftingInput blueprintInput() {
        return CraftingInput.of(3, 3, new ArrayList<>(invBlueprint.getItems()));
    }

    private void updateRecipe(ServerLevel server) {
        CraftingInput input = blueprintInput();
        Optional<RecipeHolder<CraftingRecipe>> found = input.isEmpty() ? Optional.empty()
            : server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
        recipe = found.orElse(null);
        preview.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input));
    }

    /** @return The material slot to take each blueprint slot's item from, or null if the materials are missing. */
    @Nullable
    private int[] findMaterials() {
        int[] used = new int[9];
        int[] source = new int[9];
        for (int i = 0; i < 9; i++) {
            ItemStack bpt = invBlueprint.getItem(i);
            source[i] = -1;
            if (bpt.isEmpty()) continue;
            for (int m = 0; m < 9; m++) {
                ItemStack material = invMaterials.getItem(m);
                if (material.getCount() > used[m] && ItemStack.isSameItemSameComponents(bpt, material)) {
                    used[m]++;
                    source[i] = m;
                    break;
                }
            }
            if (source[i] < 0) return null;
        }
        return source;
    }

    private boolean canCraft() {
        if (recipe == null) return false;
        ItemStack result = preview.getItem(0);
        ItemStack current = invResult.getItem(0);
        if (!current.isEmpty() && (!ItemStack.isSameItemSameComponents(current, result)
            || current.getCount() + result.getCount() > current.getMaxStackSize())) {
            return false;
        }
        return findMaterials() != null;
    }

    private void craft(ServerLevel server) {
        int[] source = findMaterials();
        if (recipe == null || source == null) return;
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            items.add(source[i] < 0 ? ItemStack.EMPTY : invMaterials.getItem(source[i]).copyWithCount(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        if (!recipe.value().matches(input, server)) return;
        ItemStack result = recipe.value().assemble(input);
        List<ItemStack> remaining = recipe.value().getRemainingItems(input);
        for (int i = 0; i < 9; i++) {
            if (source[i] >= 0) {
                invMaterials.removeItem(source[i], 1);
            }
        }
        ItemStack current = invResult.getItem(0);
        if (current.isEmpty()) {
            invResult.setItem(0, result);
        } else {
            invResult.forceInsert(0, result);
        }
        for (ItemStack left : remaining) {
            if (left.isEmpty()) continue;
            ItemStack rest = left;
            for (int m = 0; m < 9 && !rest.isEmpty(); m++) {
                if (invMaterials.canPlaceItem(m, rest)) {
                    rest = invMaterials.forceInsert(m, rest);
                }
            }
            if (!rest.isEmpty()) {
                InventoryUtil.addToBestAcceptor(server, worldPosition, rest);
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
        invBlueprint.save(output, "blueprint");
        invMaterials.save(output, "materials");
        invResult.save(output, "result");
        output.putLong("power", powerStored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invBlueprint.load(input, "blueprint");
        invMaterials.load(input, "materials");
        invResult.load(input, "result");
        powerStored = input.getLongOr("power", 0);
        recipeDirty = true;
    }

    private final class Receiver implements IMjRedstoneReceiver {
        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return Math.max(0, POWER_REQUIRED - powerStored);
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            long taken = Math.min(getPowerRequested(), microJoules);
            if (!simulate) {
                powerStored += taken;
            }
            return microJoules - taken;
        }
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerAutoWorkbench(id, inventory, this);
    }

    // WorldlyContainer: materials go in, the result comes out

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < RESULT_SLOT && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESULT_SLOT;
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        return invMaterials.isEmpty() && invResult.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == RESULT_SLOT ? invResult.getItem(0) : invMaterials.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return slot == RESULT_SLOT ? invResult.removeItem(0, count) : invMaterials.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == RESULT_SLOT ? invResult.removeItemNoUpdate(0) : invMaterials.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == RESULT_SLOT) {
            invResult.setItem(0, stack);
        } else {
            invMaterials.setItem(slot, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < RESULT_SLOT && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return invMaterials.stillValid(player);
    }

    @Override
    public void clearContent() {
        invMaterials.clearContent();
        invResult.clearContent();
    }
}
