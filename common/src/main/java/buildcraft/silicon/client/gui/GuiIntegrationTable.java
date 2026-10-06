/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.silicon.container.ContainerIntegrationTable;

public class GuiIntegrationTable extends GuiBC<ContainerIntegrationTable> {
    public GuiIntegrationTable(ContainerIntegrationTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/integration_table.png"), 176, 191);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 8;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        long target = menu.target.getLong();
        if (target > 0) {
            double v = Math.min(1, menu.power.getLong() / (double) target);
            int h = (int) Math.ceil(70 * v);
            blitPart(graphics, 164, 22 + 70 - h, 176, 70 - h, 4, h);
        }
    }
}
