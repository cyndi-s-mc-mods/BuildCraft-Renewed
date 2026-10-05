package buildcraft.lib.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.InventoryUtil;

/** Crafts the recipe laid out in a 3x3 blueprint of ghost items, taking the real items from a materials inventory.
 * Used by the auto workbench and the advanced crafting table. */
public class WorkbenchCrafting {
    private final ItemHandlerSimple blueprint;
    private final ItemHandlerSimple materials;
    private final ItemHandlerSimple results;
    /** What the blueprint makes, for showing in GUIs. */
    public final SimpleContainer preview = new SimpleContainer(1);
    @Nullable
    private RecipeHolder<CraftingRecipe> recipe;
    private boolean dirty = true;

    public WorkbenchCrafting(ItemHandlerSimple blueprint, ItemHandlerSimple materials, ItemHandlerSimple results) {
        this.blueprint = blueprint;
        this.materials = materials;
        this.results = results;
    }

    /** Call when the blueprint changes. Also keeps every blueprint stack at a count of 1. */
    public void onBlueprintChanged() {
        for (int i = 0; i < blueprint.getContainerSize(); i++) {
            ItemStack stack = blueprint.getItem(i);
            if (stack.getCount() > 1) {
                blueprint.getItems().set(i, stack.copyWithCount(1));
            }
        }
        dirty = true;
    }

    public void markDirty() {
        dirty = true;
    }

    /** @return True if the stack is used by the blueprint, and so may go in the materials inventory. */
    public boolean isMaterial(ItemStack stack) {
        for (ItemStack bpt : blueprint.getItems()) {
            if (!bpt.isEmpty() && ItemStack.isSameItemSameComponents(bpt, stack)) return true;
        }
        return false;
    }

    public void tick(ServerLevel level) {
        if (dirty) {
            dirty = false;
            CraftingInput input = CraftingInput.of(3, 3, new ArrayList<>(blueprint.getItems()));
            Optional<RecipeHolder<CraftingRecipe>> found = input.isEmpty() ? Optional.empty()
                : level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            recipe = found.orElse(null);
            preview.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input));
        }
    }

    public ItemStack getAssumedResult() {
        return preview.getItem(0);
    }

    /** @return The material slot to take each blueprint slot's item from, or null if some are missing. */
    private int @Nullable [] findMaterials() {
        int[] used = new int[materials.getContainerSize()];
        int[] source = new int[9];
        for (int i = 0; i < 9; i++) {
            ItemStack bpt = blueprint.getItem(i);
            source[i] = -1;
            if (bpt.isEmpty()) continue;
            for (int m = 0; m < materials.getContainerSize(); m++) {
                ItemStack material = materials.getItem(m);
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

    private boolean hasSpaceFor(ItemStack result) {
        int left = result.getCount();
        for (int i = 0; i < results.getContainerSize() && left > 0; i++) {
            ItemStack current = results.getItem(i);
            if (current.isEmpty()) {
                left -= result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(current, result)) {
                left -= current.getMaxStackSize() - current.getCount();
            }
        }
        return left <= 0;
    }

    public boolean canCraft() {
        return recipe != null && hasSpaceFor(getAssumedResult()) && findMaterials() != null;
    }

    /** Crafts one recipe's worth. Leftovers (such as empty buckets) go back into the materials, or out of the block.
     * @return True if something was crafted. */
    public boolean craft(ServerLevel level, BlockPos pos) {
        int[] source = findMaterials();
        if (recipe == null || source == null) return false;
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            items.add(source[i] < 0 ? ItemStack.EMPTY : materials.getItem(source[i]).copyWithCount(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        if (!recipe.value().matches(input, level)) return false;
        ItemStack result = recipe.value().assemble(input);
        List<ItemStack> remaining = recipe.value().getRemainingItems(input);
        for (int i = 0; i < 9; i++) {
            if (source[i] >= 0) {
                materials.removeItem(source[i], 1);
            }
        }
        for (int i = 0; i < results.getContainerSize() && !result.isEmpty(); i++) {
            result = results.forceInsert(i, result);
        }
        if (!result.isEmpty()) {
            InventoryUtil.addToBestAcceptor(level, pos, result);
        }
        for (ItemStack left : remaining) {
            ItemStack rest = left;
            for (int m = 0; m < materials.getContainerSize() && !rest.isEmpty(); m++) {
                if (materials.canPlaceItem(m, rest)) {
                    rest = materials.forceInsert(m, rest);
                }
            }
            if (!rest.isEmpty()) {
                InventoryUtil.addToBestAcceptor(level, pos, rest);
            }
        }
        return true;
    }
}
