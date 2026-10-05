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
