/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Lets a block entity be used as one of its inventories by hoppers, pipes and other mods. Items can be put into any slot
 * that accepts them, and taken from any slot that {@link #canTake} allows. */
public interface IContainerDelegate extends WorldlyContainer {
    Container getDelegate();

    default boolean canTake(int slot) {
        return true;
    }

    @Override
    default int[] getSlotsForFace(Direction side) {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return getDelegate().canPlaceItem(slot, stack);
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canTake(slot);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return getDelegate().canPlaceItem(slot, stack);
    }

    @Override
    default int getContainerSize() {
        return getDelegate().getContainerSize();
    }

    @Override
    default boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return getDelegate().getItem(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return getDelegate().removeItem(slot, count);
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return getDelegate().removeItemNoUpdate(slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        getDelegate().setItem(slot, stack);
    }

    @Override
    default boolean stillValid(Player player) {
        return getDelegate().stillValid(player);
    }

    @Override
    default void clearContent() {
        getDelegate().clearContent();
    }
}
