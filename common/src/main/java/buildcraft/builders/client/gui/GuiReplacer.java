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
import buildcraft.builders.container.ContainerReplacer;
import buildcraft.lib.gui.GuiBC;

public class GuiReplacer extends GuiBC<ContainerReplacer> {
    public GuiReplacer(ContainerReplacer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/replacer.png"), 176, 241);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 12;
        titleLabelY = 12;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.textWithWordWrap(font, Component.translatable("gui.buildcraft.replacer.help"), 12, 28, 152, 0xFFE0E0E0, false);
    }
}
