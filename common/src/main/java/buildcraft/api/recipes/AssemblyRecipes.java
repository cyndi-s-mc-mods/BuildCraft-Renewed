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

    private static final Map<String, AssemblyRecipe> RECIPES = new LinkedHashMap<>();

    private AssemblyRecipes() {}

    public static void register(String name, long powerRequired, List<IngredientStack> inputs, Supplier<ItemStack> output) {
        RECIPES.put(name, new AssemblyRecipe(name, powerRequired, List.copyOf(inputs), output));
    }

    public static void register(String name, long powerRequired, List<IngredientStack> inputs, ItemLike output, int count) {
        register(name, powerRequired, inputs, () -> new ItemStack(output, count));
    }

    public static Collection<AssemblyRecipe> getAll() {
        return Collections.unmodifiableCollection(RECIPES.values());
    }

    @Nullable
    public static AssemblyRecipe get(String name) {
        return RECIPES.get(name);
    }
}
