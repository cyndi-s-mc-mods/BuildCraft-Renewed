/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.IItemPluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.silicon.BCSiliconComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.PluggableLens;

/** A lens or filter. Its colour (if it has one) is stored in a data component. */
public class ItemPluggableLens extends Item implements IItemPluggable {
    private final boolean isFilter;

    public ItemPluggableLens(Properties properties, boolean isFilter) {
        super(properties);
        this.isFilter = isFilter;
    }

    @Nullable
    public static DyeColor getColour(ItemStack stack) {
        return stack.get(BCSiliconComponents.LENS_COLOUR.get());
    }

    public static ItemStack getStack(@Nullable DyeColor colour, boolean filter) {
        ItemStack stack = new ItemStack(filter ? BCSiliconItems.FILTER.get() : BCSiliconItems.LENS.get());
        if (colour != null) {
            stack.set(BCSiliconComponents.LENS_COLOUR.get(), colour);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor colour = getColour(stack);
        Component base = super.getName(stack);
        if (colour == null) return base;
        return Component.translatable("item.buildcraft.lens.coloured", Component.translatable("color.minecraft." + colour.getSerializedName()), base);
    }

    @Override
    public @Nullable PipePluggable onPlace(ItemStack stack, IPipeHolder holder, Direction side, Player player, InteractionHand hand) {
        return new PluggableLens(BCSiliconPlugs.LENS, holder, side, getColour(stack), isFilter);
    }
}
