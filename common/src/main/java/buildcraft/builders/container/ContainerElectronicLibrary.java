/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotBase;

/** The list itself is read from the library on the client (it's sent with the block's data); this syncs its position and
 * which entry is selected. */
public class ContainerElectronicLibrary extends ContainerBC<TileElectronicLibrary> {
    /** Plus the index of the entry. */
    public static final int BUTTON_SELECT = 0;
    /** Plus the index of the entry. */
    public static final int BUTTON_DELETE = TileElectronicLibrary.MAX_ENTRIES;

    public final MenuData.Field posX, posY, posZ;
    public final MenuData.Field selected;

    /** Client constructor. */
    public ContainerElectronicLibrary(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerElectronicLibrary(int id, Inventory inventory, @Nullable TileElectronicLibrary tile) {
        super(BCBuildersMenus.LIBRARY.get(), id, inventory, tile);
        posX = data.addInt(tile == null ? null : () -> tile.getBlockPos().getX());
        posY = data.addInt(tile == null ? null : () -> tile.getBlockPos().getY());
        posZ = data.addInt(tile == null ? null : () -> tile.getBlockPos().getZ());
        selected = data.addInt(tile == null ? null : tile::getSelected);
        addSlot(output(tile != null ? tile.invDownOut : new SimpleContainer(1), 175, 57));
        addSlot(new SlotBase(tile != null ? tile.invDownIn : new SimpleContainer(1), 0, 219, 57));
        addSlot(new SlotBase(tile != null ? tile.invUpIn : new SimpleContainer(1), 0, 175, 79));
        addSlot(output(tile != null ? tile.invUpOut : new SimpleContainer(1), 219, 79));
        addPlayerInventory(138);
    }

    private static SlotBase output(net.minecraft.world.Container container, int x, int y) {
        return new SlotBase(container, 0, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    public BlockPos getLibraryPos() {
        return new BlockPos(posX.getInt(), posY.getInt(), posZ.getInt());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (tile == null) return false;
        if (id >= BUTTON_DELETE) {
            tile.remove(id - BUTTON_DELETE);
        } else {
            tile.select(id - BUTTON_SELECT);
        }
        return true;
    }
}
