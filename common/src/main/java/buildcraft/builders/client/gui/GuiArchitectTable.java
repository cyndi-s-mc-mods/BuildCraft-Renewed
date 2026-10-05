/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.builders.container.ContainerArchitectTable;
import buildcraft.lib.gui.GuiBC;

public class GuiArchitectTable extends GuiBC<ContainerArchitectTable> {
    public GuiArchitectTable(ContainerArchitectTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/architect.png"), 256, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        inventoryLabelX = 88;
        inventoryLabelY = 73;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        int progress = menu.progress.getInt();
        if (progress > 0) {
            blitPart(graphics, 159, 34, 0, 166, Math.max(1, 24 * progress / 1000), 16);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component status = Component.translatable(!menu.hasBox.getBoolean() ? "gui.buildcraft.architect.no_area"
            : menu.progress.getInt() >= 0 ? "gui.buildcraft.architect.scanning" : "gui.buildcraft.architect.ready");
        graphics.textWithWordWrap(font, status, 8, 24, 76, 0xFF404040, false);
        if (menu.progress.getInt() >= 0) {
            graphics.text(font, (menu.progress.getInt() / 10) + "%", 8, 60, 0xFF404040, false);
        }
    }
}
