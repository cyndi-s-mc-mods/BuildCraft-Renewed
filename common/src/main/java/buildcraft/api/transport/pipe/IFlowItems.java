/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public interface IFlowItems {
    /** Pulls items out of the inventory on a side and into the pipe.
     * @return The number of items extracted. */
    int tryExtractItems(int count, Direction from, @Nullable DyeColor colour, Predicate<ItemStack> filter, boolean simulate);

    /** @return The items that didn't fit. */
    ItemStack injectItem(ItemStack stack, boolean doAdd, Direction from, @Nullable DyeColor colour, double speed);

    /** Adds items to the centre of the pipe without any checks. */
    void insertItemsForce(ItemStack stack, @Nullable Direction from, @Nullable DyeColor colour, double speed);
}
