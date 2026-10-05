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
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotBase;

public class ContainerArchitectTable extends ContainerBC<TileArchitectTable> {
    public final MenuData.Field progress;
    public final MenuData.Field hasBox;

    /** Client constructor. */
    public ContainerArchitectTable(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerArchitectTable(int id, Inventory inventory, @Nullable TileArchitectTable tile) {
        super(BCBuildersMenus.ARCHITECT.get(), id, inventory, tile);
        progress = data.addInt(tile == null ? null : tile::getProgress);
        hasBox = data.addBoolean(tile == null ? null : () -> tile.getBox() != null);
        addSlot(new SlotBase(tile != null ? tile.invIn : new SimpleContainer(1), 0, 135, 35));
        addSlot(new SlotBase(tile != null ? tile.invOut : new SimpleContainer(1), 0, 194, 35) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(88, 84);
    }
}
