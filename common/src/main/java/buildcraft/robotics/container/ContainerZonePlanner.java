/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.robotics.BCRoboticsMenus;
import buildcraft.robotics.tile.TileZonePlanner;

public class ContainerZonePlanner extends ContainerBC<TileZonePlanner> {
    public final MenuData.Field progressInput, progressOutput;
    private final MenuData.Field posX, posY, posZ;

    /** Client constructor. */
    public ContainerZonePlanner(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerZonePlanner(int id, Inventory inventory, @Nullable TileZonePlanner tile) {
        super(BCRoboticsMenus.ZONE_PLANNER.get(), id, inventory, tile);
        Container paintbrushes = tile != null ? tile.invPaintbrushes : new SimpleContainer(16);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                addSlot(new Checked(paintbrushes, x * 4 + y, 8 + x * 18, 146 + y * 18));
            }
        }
        addSlot(new Checked(tile != null ? tile.invInputPaintbrush : new SimpleContainer(1), 0, 8, 125));
        addSlot(new Checked(tile != null ? tile.invInputMap : new SimpleContainer(1), 0, 26, 125));
        addSlot(new Checked(tile != null ? tile.invInputResult : new SimpleContainer(1), 0, 74, 125));
        addSlot(new Checked(tile != null ? tile.invOutputPaintbrush : new SimpleContainer(1), 0, 233, 9));
        addSlot(new Checked(tile != null ? tile.invOutputMap : new SimpleContainer(1), 0, 233, 27));
        addSlot(new Checked(tile != null ? tile.invOutputResult : new SimpleContainer(1), 0, 233, 75));
        progressInput = data.addInt(tile == null ? null : tile::getProgressInput);
        progressOutput = data.addInt(tile == null ? null : tile::getProgressOutput);
        posX = data.addInt(tile == null ? null : () -> tile.getBlockPos().getX());
        posY = data.addInt(tile == null ? null : () -> tile.getBlockPos().getY());
        posZ = data.addInt(tile == null ? null : () -> tile.getBlockPos().getZ());
        addPlayerInventory(88, 146);
    }

    public BlockPos getPlannerPos() {
        return new BlockPos(posX.getInt(), posY.getInt(), posZ.getInt());
    }

    /** A slot that only accepts what its inventory does. */
    private static class Checked extends Slot {
        Checked(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
