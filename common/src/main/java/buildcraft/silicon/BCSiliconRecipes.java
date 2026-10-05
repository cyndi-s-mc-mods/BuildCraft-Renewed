package buildcraft.silicon;

import java.util.List;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipes;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.transport.BCTransportItems;

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

        // Gates
        IngredientStack ironChip = IngredientStack.of(BCSiliconItems.CHIPSET_IRON.get());
        IngredientStack goldChip = IngredientStack.of(BCSiliconItems.CHIPSET_GOLD.get());
        IngredientStack quartzChip = IngredientStack.of(BCSiliconItems.CHIPSET_QUARTZ.get());
        IngredientStack diamondChip = IngredientStack.of(BCSiliconItems.CHIPSET_DIAMOND.get());
        IngredientStack lapis = IngredientStack.tag("c:gems/lapis");
        gate(20_000, GateVariant.Material.IRON, List.of(ironChip));
        gate(40_000, GateVariant.Material.NETHER_BRICK, List.of(ironChip, IngredientStack.of(Items.NETHER_BRICKS)));
        gate(80_000, GateVariant.Material.GOLD, List.of(goldChip));
        modifier(40_000, GateVariant.Material.IRON, GateVariant.Modifier.LAPIS, lapis);
        modifier(60_000, GateVariant.Material.IRON, GateVariant.Modifier.QUARTZ, quartzChip);
        modifier(80_000, GateVariant.Material.IRON, GateVariant.Modifier.DIAMOND, diamondChip);
        modifier(80_000, GateVariant.Material.NETHER_BRICK, GateVariant.Modifier.LAPIS, lapis);
        modifier(100_000, GateVariant.Material.NETHER_BRICK, GateVariant.Modifier.QUARTZ, quartzChip);
        modifier(120_000, GateVariant.Material.NETHER_BRICK, GateVariant.Modifier.DIAMOND, diamondChip);
        modifier(100_000, GateVariant.Material.GOLD, GateVariant.Modifier.LAPIS, lapis);
        modifier(140_000, GateVariant.Material.GOLD, GateVariant.Modifier.QUARTZ, quartzChip);
        modifier(180_000, GateVariant.Material.GOLD, GateVariant.Modifier.DIAMOND, diamondChip);

        // Pipe wires: 8 from redstone and a dye
        for (DyeColor colour : DyeColor.values()) {
            AssemblyRecipes.register("wire_" + colour.getSerializedName(), 10_000 * MjAPI.MJ,
                List.of(redstone, IngredientStack.tag("c:dyes/" + colour.getSerializedName())), BCTransportItems.WIRES.get(colour).get(), 8);
        }
    }

    private static void gate(long mj, GateVariant.Material material, List<IngredientStack> inputs) {
        for (GateVariant.Logic logic : GateVariant.Logic.values()) {
            GateVariant variant = new GateVariant(logic, material, GateVariant.Modifier.NO_MODIFIER);
            AssemblyRecipes.register("gate_" + variant.getName(), mj * MjAPI.MJ, inputs, () -> ItemPluggableGate.getStack(variant));
        }
    }

    private static void modifier(long mj, GateVariant.Material material, GateVariant.Modifier modifier, IngredientStack extra) {
        for (GateVariant.Logic logic : GateVariant.Logic.values()) {
            GateVariant from = new GateVariant(logic, material, GateVariant.Modifier.NO_MODIFIER);
            GateVariant to = new GateVariant(logic, material, modifier);
            AssemblyRecipes.register("gate_" + to.getName(), mj * MjAPI.MJ,
                List.of(IngredientStack.exact(() -> ItemPluggableGate.getStack(from)), extra), () -> ItemPluggableGate.getStack(to));
        }
    }
}
