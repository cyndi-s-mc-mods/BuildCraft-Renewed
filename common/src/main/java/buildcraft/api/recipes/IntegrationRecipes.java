/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.recipes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/** Recipes for the integration table, which is powered by lasers: an item in the centre slot is combined with items
 * around it. BuildCraft itself adds none; other mods (and modpacks) register theirs here. */
public final class IntegrationRecipes {
    public interface IntegrationRecipe {
        String name();

        /** @return What the given items make, or an empty stack if this recipe doesn't use them. */
        ItemStack getOutput(ItemStack target, List<ItemStack> toIntegrate);

        /** @return How much of the centre item one use of the recipe takes. */
        IngredientStack getTarget();

        /** @return The items (from around the centre) that making the given output takes. */
        List<IngredientStack> getRequirements(ItemStack output);

        /** @return The power, in micro joules, needed to make the given output. */
        long getPower(ItemStack output);
    }

    /** A recipe with a fixed output and fixed ingredients. */
    public record Basic(String name, long power, IngredientStack target, List<IngredientStack> toIntegrate,
        Supplier<ItemStack> output) implements IntegrationRecipe {

        @Override
        public ItemStack getOutput(ItemStack centre, List<ItemStack> around) {
            if (!target.test(centre) || centre.getCount() < target.count()) return ItemStack.EMPTY;
            for (IngredientStack ingredient : toIntegrate) {
                int found = 0;
                for (ItemStack stack : around) {
                    if (ingredient.test(stack)) found += stack.getCount();
                }
                if (found < ingredient.count()) return ItemStack.EMPTY;
            }
            return output.get().copy();
        }

        @Override
        public IngredientStack getTarget() {
            return target;
        }

        @Override
        public List<IngredientStack> getRequirements(ItemStack made) {
            return toIntegrate;
        }

        @Override
        public long getPower(ItemStack made) {
            return power;
        }
    }

    private static final Map<String, IntegrationRecipe> RECIPES = new LinkedHashMap<>();

    private IntegrationRecipes() {}

    public static void register(IntegrationRecipe recipe) {
        RECIPES.put(recipe.name(), recipe);
    }

    @Nullable
    public static IntegrationRecipe get(String name) {
        return RECIPES.get(name);
    }

    /** @return The first recipe that can use the given items, or null. */
    @Nullable
    public static IntegrationRecipe find(ItemStack target, List<ItemStack> toIntegrate) {
        if (target.isEmpty()) return null;
        for (IntegrationRecipe recipe : RECIPES.values()) {
            if (!recipe.getOutput(target, toIntegrate).isEmpty()) return recipe;
        }
        return null;
    }
}
