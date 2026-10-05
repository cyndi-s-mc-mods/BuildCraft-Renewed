/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.fuels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import buildcraft.lib.fluid.BCFluidStack;

/** Fuels for the combustion engine, and coolants to keep it from overheating. */
public final class BuildcraftFuelRegistry {
    /** A fluid that the combustion engine can burn.
     *
     * @param powerPerCycle Micro MJ produced each tick while burning.
     * @param totalBurningTime Ticks that one bucket (1000 mB) burns for.
     * @param residue What's left over after burning a bucket, or empty. */
    public record Fuel(Fluid fluid, long powerPerCycle, int totalBurningTime, BCFluidStack residue) {
        public boolean isDirty() {
            return !residue.isEmpty();
        }
    }

    /** @param degreesPerMb How much each mB cools the engine. */
    public record Coolant(Fluid fluid, float degreesPerMb) {}

    /** An item, such as ice, that turns into a fluid coolant. */
    public record SolidCoolant(Item item, BCFluidStack fluid, float multiplier) {}

    private static final List<Fuel> FUELS = new ArrayList<>();
    private static final List<Coolant> COOLANTS = new ArrayList<>();
    private static final List<SolidCoolant> SOLID_COOLANTS = new ArrayList<>();

    private BuildcraftFuelRegistry() {}

    public static void addFuel(Fluid fluid, long powerPerCycle, int totalBurningTime) {
        FUELS.add(new Fuel(fluid, powerPerCycle, totalBurningTime, BCFluidStack.EMPTY));
    }

    public static void addDirtyFuel(Fluid fluid, long powerPerCycle, int totalBurningTime, BCFluidStack residue) {
        FUELS.add(new Fuel(fluid, powerPerCycle, totalBurningTime, residue));
    }

    public static void addCoolant(Fluid fluid, float degreesPerMb) {
        COOLANTS.add(new Coolant(fluid, degreesPerMb));
    }

    public static void addSolidCoolant(Item item, BCFluidStack fluid, float multiplier) {
        SOLID_COOLANTS.add(new SolidCoolant(item, fluid, multiplier));
    }

    public static List<Fuel> getFuels() {
        return Collections.unmodifiableList(FUELS);
    }

    @Nullable
    public static Fuel getFuel(BCFluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (Fuel fuel : FUELS) {
            if (fluid.isSameFluid(fuel.fluid())) return fuel;
        }
        return null;
    }

    @Nullable
    public static Coolant getCoolant(BCFluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (Coolant coolant : COOLANTS) {
            if (fluid.isSameFluid(coolant.fluid())) return coolant;
        }
        return null;
    }

    /** @param heat The current heat of the engine. */
    public static float getDegreesPerMb(BCFluidStack fluid, float heat) {
        Coolant coolant = getCoolant(fluid);
        return coolant == null ? 0 : coolant.degreesPerMb();
    }

    @Nullable
    public static SolidCoolant getSolidCoolant(ItemStack stack) {
        for (SolidCoolant coolant : SOLID_COOLANTS) {
            if (stack.is(coolant.item())) return coolant;
        }
        return null;
    }
}
