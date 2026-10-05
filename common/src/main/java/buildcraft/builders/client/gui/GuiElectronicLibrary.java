/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.builders.container.ContainerElectronicLibrary;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.lib.gui.GuiBC;

/** Lists the library's snapshots: click one to select it (it is then written onto items put in the top right slot), and
 * shift click one to remove it. */
public class GuiElectronicLibrary extends GuiBC<ContainerElectronicLibrary> {
    private static final int LIST_X = 8, LIST_Y = 22, LIST_W = 154, LIST_H = 108, ROW_H = 12;
    private static final int ROWS = LIST_H / ROW_H;
    private int scroll = 0;

    public GuiElectronicLibrary(ContainerElectronicLibrary menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/electronic_library.png"), 244, 220);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 8;
        // There's no room for the label between the list and the inventory
        inventoryLabelY = -1000;
    }

    private List<SnapshotHeader> entries() {
        if (minecraft != null && minecraft.level != null
            && minecraft.level.getBlockEntity(menu.getLibraryPos()) instanceof TileElectronicLibrary library) {
            return library.getEntries();
        }
        return List.of();
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        List<SnapshotHeader> entries = entries();
        scroll = Math.max(0, Math.min(scroll, entries.size() - ROWS));
        int selected = menu.selected.getInt();
        for (int row = 0; row < ROWS && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            SnapshotHeader header = entries.get(index);
            int x = leftPos + LIST_X, y = topPos + LIST_Y + row * ROW_H;
            if (index == selected) {
                graphics.fill(x, y, x + LIST_W, y + ROW_H, 0xFF606090);
            } else if (isHovering(LIST_X, LIST_Y + row * ROW_H, LIST_W, ROW_H, mouseX, mouseY)) {
                graphics.fill(x, y, x + LIST_W, y + ROW_H, 0x40FFFFFF);
            }
            String kind = header.type() == Snapshot.Type.BLUEPRINT ? "B" : "T";
            String name = header.name().isEmpty() ? header.sizeText() : header.name() + " (" + header.sizeText() + ")";
            graphics.text(font, kind + " " + font.plainSubstrByWidth(name, LIST_W - 14), x + 2, y + 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isHovering(LIST_X, LIST_Y, LIST_W, LIST_H, mouseX, mouseY)) {
            scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (isHovering(LIST_X, LIST_Y, LIST_W, LIST_H, mx, my)) {
            int index = scroll + (int) ((my - topPos - LIST_Y) / ROW_H);
            if (index < entries().size()) {
                sendButtonClick((event.hasShiftDown() ? ContainerElectronicLibrary.BUTTON_DELETE : ContainerElectronicLibrary.BUTTON_SELECT)
                    + index);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
