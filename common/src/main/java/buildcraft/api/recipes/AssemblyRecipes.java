/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.recipes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Recipes for the assembly table, which is powered by lasers. */
public final class AssemblyRecipes {
    /** The output is made when needed, as item stacks can't be made until the game has loaded. */
    public record AssemblyRecipe(String name, long powerRequired, List<IngredientStack> inputs, Supplier<ItemStack> outputFactory) {
        public ItemStack output() {
            return outputFactory.get();
        }
    }

    /** Makes recipes that depend on the items given, such as facades (one for each block). */
    public interface IRecipeProvider {
        /** @return The recipes that could use any of the given items. */
        List<AssemblyRecipe> getRecipesFor(List<ItemStack> inputs);

        /** @return The recipe with the given name, if this made it. */
        @Nullable
        AssemblyRecipe get(String name);
    }

    private static final Map<String, AssemblyRecipe> RECIPES = new LinkedHashMap<>();
    private static final List<IRecipeProvider> PROVIDERS = new java.util.ArrayList<>();

    private AssemblyRecipes() {}

    public static void register(String name, long powerRequired, List<IngredientStack> inputs, Supplier<ItemStack> output) {
        RECIPES.put(name, new AssemblyRecipe(name, powerRequired, List.copyOf(inputs), output));
    }

    public static void register(String name, long powerRequired, List<IngredientStack> inputs, ItemLike output, int count) {
        register(name, powerRequired, inputs, () -> new ItemStack(output, count));
    }

    public static void registerProvider(IRecipeProvider provider) {
        PROVIDERS.add(provider);
    }

    /** @return The fixed recipes, followed by the ones the providers make for the given items. */
    public static List<AssemblyRecipe> getAllFor(List<ItemStack> inputs) {
        List<AssemblyRecipe> list = new java.util.ArrayList<>(RECIPES.values());
        for (IRecipeProvider provider : PROVIDERS) {
            list.addAll(provider.getRecipesFor(inputs));
        }
        return list;
    }

    public static Collection<AssemblyRecipe> getAll() {
        return Collections.unmodifiableCollection(RECIPES.values());
    }

    @Nullable
    public static AssemblyRecipe get(String name) {
        AssemblyRecipe recipe = RECIPES.get(name);
        if (recipe != null) return recipe;
        for (IRecipeProvider provider : PROVIDERS) {
            recipe = provider.get(name);
            if (recipe != null) return recipe;
        }
        return null;
    }
}
