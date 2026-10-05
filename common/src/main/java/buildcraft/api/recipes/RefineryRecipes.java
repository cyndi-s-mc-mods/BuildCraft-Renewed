/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import buildcraft.lib.fluid.BCFluidStack;

/** Recipes for the distiller and the heat exchanger. */
public final class RefineryRecipes {
    /** Splits one fluid into a gas (going up) and a liquid (going down). The amounts are per operation. */
    public record Distillation(BCFluidStack in, BCFluidStack outGas, BCFluidStack outLiquid, long powerRequired) {}

    /** A fluid that is heated from one heat level to another. A null output means the fluid is used up (such as water
     * turning to steam). */
    public record Heatable(BCFluidStack in, @Nullable BCFluidStack out, int heatFrom, int heatTo) {}

    /** A fluid that is cooled from one heat level to another, heating the other fluid. A null output means the fluid is
     * used up. */
    public record Coolable(BCFluidStack in, @Nullable BCFluidStack out, int heatFrom, int heatTo) {}

    private static final List<Distillation> DISTILLATIONS = new ArrayList<>();
    private static final List<Heatable> HEATABLES = new ArrayList<>();
    private static final List<Coolable> COOLABLES = new ArrayList<>();

    private RefineryRecipes() {}

    public static void addDistillation(BCFluidStack in, BCFluidStack outGas, BCFluidStack outLiquid, long powerRequired) {
        if (getDistillation(in) != null) {
            throw new IllegalStateException("Already added a distillation recipe for " + in);
        }
        DISTILLATIONS.add(new Distillation(in, outGas, outLiquid, powerRequired));
    }

    public static void addHeatable(BCFluidStack in, @Nullable BCFluidStack out, int heatFrom, int heatTo) {
        HEATABLES.add(new Heatable(in, out, heatFrom, heatTo));
    }

    public static void addCoolable(BCFluidStack in, @Nullable BCFluidStack out, int heatFrom, int heatTo) {
        COOLABLES.add(new Coolable(in, out, heatFrom, heatTo));
    }

    @Nullable
    public static Distillation getDistillation(BCFluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (Distillation recipe : DISTILLATIONS) {
            if (recipe.in().isSameFluid(fluid)) return recipe;
        }
        return null;
    }

    @Nullable
    public static Heatable getHeatable(BCFluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (Heatable recipe : HEATABLES) {
            if (recipe.in().isSameFluid(fluid)) return recipe;
        }
        return null;
    }

    @Nullable
    public static Coolable getCoolable(BCFluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (Coolable recipe : COOLABLES) {
            if (recipe.in().isSameFluid(fluid)) return recipe;
        }
        return null;
    }

    public static List<Distillation> getDistillations() {
        return Collections.unmodifiableList(DISTILLATIONS);
    }
}
