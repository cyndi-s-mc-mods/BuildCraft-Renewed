/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotDisplay;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.tile.TileIntegrationTable;

public class ContainerIntegrationTable extends ContainerBC<TileIntegrationTable> {
    public final MenuData.Field power;
    public final MenuData.Field target;

    /** Client constructor. */
    public ContainerIntegrationTable(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerIntegrationTable(int id, Inventory inventory, @Nullable TileIntegrationTable tile) {
        super(BCSiliconMenus.INTEGRATION_TABLE.get(), id, inventory, tile);
        Container centre = tile != null ? tile.invTarget : new SimpleContainer(1);
        Container around = tile != null ? tile.invToIntegrate : new SimpleContainer(TileIntegrationTable.AROUND);
        Container result = tile != null ? tile.invResult : new SimpleContainer(1);
        Container preview = tile != null ? tile.preview : new SimpleContainer(1);
        int[] indexes = { 0, 1, 2, 3, 0, 4, 5, 6, 7 };
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                boolean isCentre = x == 1 && y == 1;
                addSlot(new Slot(isCentre ? centre : around, indexes[x + y * 3], 19 + x * 25, 24 + y * 25));
            }
        }
        addSlot(new SlotDisplay(preview, 0, 101, 36));
        addSlot(new Slot(result, 0, 138, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        power = data.addLong(tile == null ? null : () -> tile.power);
        target = data.addLong(tile == null ? null : tile::getTarget);
        addPlayerInventory(109);
    }
}
