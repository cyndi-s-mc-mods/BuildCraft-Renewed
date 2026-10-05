/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.function.Predicate;

import net.minecraft.core.Direction;

import buildcraft.lib.fluid.BCFluidStack;

public interface IFlowFluid {
    /** Pulls fluid out of the tank on a side and into the pipe.
     * @return The fluid extracted, or empty. */
    BCFluidStack tryExtractFluid(int millibuckets, Direction from, Predicate<BCFluidStack> filter, boolean simulate);
}
