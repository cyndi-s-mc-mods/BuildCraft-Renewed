package buildcraft.silicon;

import java.util.List;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipes;
import buildcraft.api.recipes.IngredientStack;

public final class BCSiliconRecipes {
    private BCSiliconRecipes() {}

    static void init() {
        IngredientStack redstone = IngredientStack.tag("c:dusts/redstone");
        AssemblyRecipes.register("redstone_chipset", 10_000 * MjAPI.MJ, List.of(redstone), BCSiliconItems.CHIPSET_REDSTONE.get(), 1);
        AssemblyRecipes.register("iron_chipset", 20_000 * MjAPI.MJ, List.of(redstone, IngredientStack.tag("c:ingots/iron")),
            BCSiliconItems.CHIPSET_IRON.get(), 1);
        AssemblyRecipes.register("gold_chipset", 40_000 * MjAPI.MJ, List.of(redstone, IngredientStack.tag("c:ingots/gold")),
            BCSiliconItems.CHIPSET_GOLD.get(), 1);
        AssemblyRecipes.register("quartz_chipset", 60_000 * MjAPI.MJ, List.of(redstone, IngredientStack.tag("c:gems/quartz")),
            BCSiliconItems.CHIPSET_QUARTZ.get(), 1);
        AssemblyRecipes.register("diamond_chipset", 80_000 * MjAPI.MJ, List.of(redstone, IngredientStack.tag("c:gems/diamond")),
            BCSiliconItems.CHIPSET_DIAMOND.get(), 1);
    }
}
