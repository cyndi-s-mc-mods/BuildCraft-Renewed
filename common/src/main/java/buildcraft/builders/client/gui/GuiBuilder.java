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
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.lib.gui.GuiBC;

public class GuiBuilder extends GuiBC<ContainerBuilder> {
    public GuiBuilder(ContainerBuilder menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/builder.png"), 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        inventoryLabelY = 129;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component status;
        if (!menu.hasPlan.getBoolean()) {
            status = Component.translatable("gui.buildcraft.builder.no_plan");
        } else if (menu.finished.getBoolean()) {
            status = Component.translatable("gui.buildcraft.filler.finished");
        } else {
            status = Component.translatable("gui.buildcraft.filler.progress", menu.toBreak.getInt(), menu.toPlace.getInt());
        }
        graphics.text(font, status, (imageWidth - font.width(status)) / 2, 50, 0xFF404040, false);
    }
}
