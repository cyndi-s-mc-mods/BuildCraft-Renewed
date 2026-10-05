/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotBase;

public class ContainerBuilder extends ContainerBC<TileBuilder> {
    public final MenuData.Field toBreak;
    public final MenuData.Field toPlace;
    public final MenuData.Field finished;
    public final MenuData.Field hasPlan;

    /** Client constructor. */
    public ContainerBuilder(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerBuilder(int id, Inventory inventory, @Nullable TileBuilder tile) {
        super(BCBuildersMenus.BUILDER.get(), id, inventory, tile);
        toBreak = data.addInt(tile == null ? null : tile::getLeftToBreak);
        toPlace = data.addInt(tile == null ? null : tile::getLeftToPlace);
        finished = data.addBoolean(tile == null ? null : tile::isFinished);
        hasPlan = data.addBoolean(tile == null ? null : () -> tile.getBox() != null);
        addSlot(new SlotBase(tile != null ? tile.invSnapshot : new SimpleContainer(1), 0, 80, 27));
        Container inv = tile != null ? tile.inv : new SimpleContainer(TileBuilder.INV_SIZE);
        for (int i = 0; i < TileBuilder.INV_SIZE; i++) {
            addSlot(new SlotBase(inv, i, 8 + (i % 9) * 18, 72 + (i / 9) * 18));
        }
        addPlayerInventory(140);
    }
}
