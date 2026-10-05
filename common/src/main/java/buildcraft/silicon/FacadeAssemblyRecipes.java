/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipes;
import buildcraft.api.recipes.AssemblyRecipes.AssemblyRecipe;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeData;
import buildcraft.silicon.plug.FacadeStates;
import buildcraft.transport.BCTransportPipes;

/** Facades are made in the assembly table from three structure pipes and the block to copy, giving six facades, solid
 * or hollow. */
public final class FacadeAssemblyRecipes implements AssemblyRecipes.IRecipeProvider {
    public static final FacadeAssemblyRecipes INSTANCE = new FacadeAssemblyRecipes();
    private static final String PREFIX = "facade/";
    private static final long MJ_COST = 64 * MjAPI.MJ;

    private FacadeAssemblyRecipes() {}

    @Override
    public List<AssemblyRecipe> getRecipesFor(List<ItemStack> inputs) {
        Set<BlockState> states = new LinkedHashSet<>();
        for (ItemStack stack : inputs) {
            states.addAll(FacadeStates.getStates(stack.getItem()));
        }
        List<AssemblyRecipe> recipes = new ArrayList<>();
        for (BlockState state : states) {
            recipes.add(create(new FacadeData(state, false)));
            recipes.add(create(new FacadeData(state, true)));
        }
        return recipes;
    }

    @Override
    public @Nullable AssemblyRecipe get(String name) {
        if (!name.startsWith(PREFIX)) return null;
        String rest = name.substring(PREFIX.length());
        boolean hollow = rest.startsWith("hollow/");
        if (hollow) rest = rest.substring("hollow/".length());
        try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, rest, false).blockState();
            return FacadeStates.isValid(state) ? create(new FacadeData(state, hollow)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static AssemblyRecipe create(FacadeData data) {
        String name = PREFIX + (data.hollow() ? "hollow/" : "") + BlockStateParser.serialize(data.state());
        List<IngredientStack> inputs = List.of(
            IngredientStack.of(BCTransportPipes.BLOCKS.get(BCTransportPipes.DEFINITIONS.get("structure")).get(), 3),
            IngredientStack.of(data.state().getBlock()));
        return new AssemblyRecipe(name, MJ_COST, inputs, () -> ItemPluggableFacade.getStack(data, 6));
    }
}
