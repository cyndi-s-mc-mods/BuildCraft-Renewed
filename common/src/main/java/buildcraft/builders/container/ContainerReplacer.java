/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.tile.TileReplacer;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.SlotBase;

public class ContainerReplacer extends ContainerBC<TileReplacer> {
    /** Client constructor. */
    public ContainerReplacer(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerReplacer(int id, Inventory inventory, @Nullable TileReplacer tile) {
        super(BCBuildersMenus.REPLACER.get(), id, inventory, tile);
        addSlot(new SlotBase(tile != null ? tile.invSnapshot : new SimpleContainer(1), 0, 8, 115));
        addSlot(new SlotBase(tile != null ? tile.invFrom : new SimpleContainer(1), 0, 8, 137));
        addSlot(new SlotBase(tile != null ? tile.invTo : new SimpleContainer(1), 0, 56, 137));
        addPlayerInventory(159);
    }
}
