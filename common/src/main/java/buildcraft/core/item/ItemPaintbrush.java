/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.core.BCCoreItems;
import buildcraft.lib.BCLibComponents;
import buildcraft.lib.item.ICreativeVariants;

/** Paints pipes (so that pipes of different colours don't connect). A clean brush removes the paint. Each brush has a
 * limited amount of paint, and can be refilled by crafting it with dye. */
public class ItemPaintbrush extends Item implements ICreativeVariants {
    public static final int MAX_USES = 64;

    public ItemPaintbrush(Properties properties) {
        super(properties.durability(MAX_USES));
    }

    @Nullable
    public static DyeColor getColour(ItemStack stack) {
        return stack.get(BCLibComponents.COLOUR.get());
    }

    public static ItemStack getStack(@Nullable DyeColor colour) {
        ItemStack stack = new ItemStack(BCCoreItems.PAINTBRUSH.get());
        if (colour != null) stack.set(BCLibComponents.COLOUR.get(), colour);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor colour = getColour(stack);
        if (colour == null) return Component.translatable("item.buildcraft.paintbrush.clean");
        return Component.translatable("item.buildcraft.paintbrush.coloured", Component.translatable("color.minecraft." + colour.getSerializedName()));
    }

    @Override
    public void addCreativeVariants(Consumer<ItemStack> output) {
        output.accept(getStack(null));
        for (DyeColor colour : DyeColor.values()) {
            output.accept(getStack(colour));
        }
    }
}
