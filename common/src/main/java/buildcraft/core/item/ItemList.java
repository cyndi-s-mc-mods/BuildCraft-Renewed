/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import buildcraft.core.container.ContainerList;
import buildcraft.lib.BCLibComponents;
import buildcraft.lib.list.ListData;

/** A list of items, which can be used in place of an item in pipe filters to match any of them. Use it to edit it. */
public class ItemList extends Item {
    public ItemList(Properties properties) {
        super(properties);
    }

    public static ListData getData(ItemStack stack) {
        ListData data = stack.get(BCLibComponents.LIST.get());
        return data == null ? ListData.EMPTY : data;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ContainerList(id, inventory, hand), stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }
}
