/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.RefineryRecipes;
import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.lib.fluid.BCFluidStack;

/** Fuels and coolants. Runs after registration, as it needs the fluid instances. */
public final class BCEnergyRecipes {
    private static final int TIME_BASE = 240_000; // 240_000 - multiple of 3, 5, 16, 1000

    // Relative amounts of the fluid -- the amount of oil used in refining will return X amount of fluid
    // single
    static final int OIL = 8;
    static final int GAS = 16;
    static final int LIGHT = 4;
    static final int DENSE = 2;
    static final int RESIDUE = 1;
    // double
    static final int GAS_LIGHT = 10;
    static final int LIGHT_DENSE = 5;
    static final int DENSE_RESIDUE = 2;
    // triple
    static final int LIGHT_DENSE_RESIDUE = 3;
    static final int GAS_LIGHT_DENSE = 8;

    private BCEnergyRecipes() {}

    static void init() {
        BuildcraftFuelRegistry.addCoolant(Fluids.WATER, 0.0023f);
        BuildcraftFuelRegistry.addSolidCoolant(Items.ICE, BCFluidStack.of(Fluids.WATER, 1000), 1.5f);
        BuildcraftFuelRegistry.addSolidCoolant(Items.PACKED_ICE, BCFluidStack.of(Fluids.WATER, 1000), 2f);

        addFuel(BCEnergyFluids.fuelGaseous, GAS, 8, 4);
        addFuel(BCEnergyFluids.fuelLight, LIGHT, 6, 6);
        addFuel(BCEnergyFluids.fuelDense, DENSE, 4, 12);
        addFuel(BCEnergyFluids.fuelMixedLight, GAS_LIGHT, 3, 5);
        addFuel(BCEnergyFluids.fuelMixedHeavy, LIGHT_DENSE, 5, 8);
        addDirtyFuel(BCEnergyFluids.oilDense, DENSE_RESIDUE, 4, 4);
        addFuel(BCEnergyFluids.oilDistilled, GAS_LIGHT_DENSE, 1, 5);
        addDirtyFuel(BCEnergyFluids.oilHeavy, LIGHT_DENSE_RESIDUE, 2, 4);
        addDirtyFuel(BCEnergyFluids.crudeOil, OIL, 3, 4);

        addDistillation(BCEnergyFluids.crudeOil, OIL, BCEnergyFluids.fuelGaseous, GAS, BCEnergyFluids.oilHeavy, LIGHT_DENSE_RESIDUE, 0, 32);
        addDistillation(BCEnergyFluids.crudeOil, OIL, BCEnergyFluids.fuelMixedLight, GAS_LIGHT, BCEnergyFluids.oilDense, DENSE_RESIDUE, 1, 16);
        addDistillation(BCEnergyFluids.crudeOil, OIL, BCEnergyFluids.oilDistilled, GAS_LIGHT_DENSE, BCEnergyFluids.oilResidue, RESIDUE, 2, 12);

        addDistillation(BCEnergyFluids.oilDistilled, GAS_LIGHT_DENSE, BCEnergyFluids.fuelGaseous, GAS, BCEnergyFluids.fuelMixedHeavy, LIGHT_DENSE, 0, 24);
        addDistillation(BCEnergyFluids.oilDistilled, GAS_LIGHT_DENSE, BCEnergyFluids.fuelMixedLight, GAS_LIGHT, BCEnergyFluids.fuelDense, DENSE, 1, 16);

        addDistillation(BCEnergyFluids.fuelMixedLight, GAS_LIGHT, BCEnergyFluids.fuelGaseous, GAS, BCEnergyFluids.fuelLight, LIGHT, 0, 24);

        addDistillation(BCEnergyFluids.oilHeavy, LIGHT_DENSE_RESIDUE, BCEnergyFluids.fuelLight, LIGHT, BCEnergyFluids.oilDense, DENSE_RESIDUE, 1, 16);
        addDistillation(BCEnergyFluids.oilHeavy, LIGHT_DENSE_RESIDUE, BCEnergyFluids.fuelMixedHeavy, LIGHT_DENSE, BCEnergyFluids.oilResidue, RESIDUE, 2, 12);

        addDistillation(BCEnergyFluids.fuelMixedHeavy, LIGHT_DENSE, BCEnergyFluids.fuelLight, LIGHT, BCEnergyFluids.fuelDense, DENSE, 1, 16);

        addDistillation(BCEnergyFluids.oilDense, DENSE_RESIDUE, BCEnergyFluids.fuelDense, DENSE, BCEnergyFluids.oilResidue, RESIDUE, 2, 12);

        for (BCFluidDefinition[] fluid : new BCFluidDefinition[][] { BCEnergyFluids.crudeOil, BCEnergyFluids.oilDistilled,
            BCEnergyFluids.oilHeavy, BCEnergyFluids.fuelMixedLight, BCEnergyFluids.fuelMixedHeavy, BCEnergyFluids.oilDense,
            BCEnergyFluids.fuelGaseous, BCEnergyFluids.fuelLight, BCEnergyFluids.fuelDense, BCEnergyFluids.oilResidue }) {
            addHeatExchange(fluid);
        }
        RefineryRecipes.addHeatable(BCFluidStack.of(Fluids.WATER, 10), null, 0, 1);
        RefineryRecipes.addCoolable(BCFluidStack.of(Fluids.LAVA, 5), null, 4, 2);
    }

    /** The amounts are relative: distilling {@code inAmount} of the input gives {@code gasAmount} of gas and
     * {@code liquidAmount} of liquid. */
    private static void addDistillation(BCFluidDefinition[] in, int inAmount, BCFluidDefinition[] gas, int gasAmount,
        BCFluidDefinition[] liquid, int liquidAmount, int heat, long mjCost) {
        int hcf = gcd(gcd(inAmount, gasAmount), liquidAmount);
        RefineryRecipes.addDistillation(BCFluidStack.of(in[heat].source.get(), inAmount / hcf),
            BCFluidStack.of(gas[heat].source.get(), gasAmount / hcf), BCFluidStack.of(liquid[heat].source.get(), liquidAmount / hcf),
            mjCost * MjAPI.MJ / hcf);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static void addHeatExchange(BCFluidDefinition[] fluid) {
        for (int i = 0; i < fluid.length - 1; i++) {
            BCFluidStack cool = BCFluidStack.of(fluid[i].source.get(), 10);
            BCFluidStack hot = BCFluidStack.of(fluid[i + 1].source.get(), 10);
            RefineryRecipes.addHeatable(cool, hot, fluid[i].heat, fluid[i + 1].heat);
            RefineryRecipes.addCoolable(hot, cool, fluid[i + 1].heat, fluid[i].heat);
        }
    }

    private static void addFuel(BCFluidDefinition[] in, int amountDiff, int multiplier, int boostOver4) {
        long powerPerCycle = multiplier * MjAPI.MJ;
        int totalTime = TIME_BASE * boostOver4 / 4 / multiplier / amountDiff;
        BuildcraftFuelRegistry.addFuel(in[0].source.get(), powerPerCycle, totalTime);
    }

    private static void addDirtyFuel(BCFluidDefinition[] in, int amountDiff, int multiplier, int boostOver4) {
        long powerPerCycle = multiplier * MjAPI.MJ;
        int totalTime = TIME_BASE * boostOver4 / 4 / multiplier / amountDiff;
        BCFluidStack residue = BCFluidStack.of(BCEnergyFluids.oilResidue[0].source.get(), 1000 / amountDiff);
        BuildcraftFuelRegistry.addDirtyFuel(in[0].source.get(), powerPerCycle, totalTime, residue);
    }
}
