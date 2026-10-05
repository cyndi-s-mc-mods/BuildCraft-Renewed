/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.core.container.ContainerList;
import buildcraft.lib.gui.GuiBC;
import buildcraft.lib.list.ListData;

public class GuiList extends GuiBC<ContainerList> {
    private static final String[] OPTIONS = { "precise", "type", "material" };
    private static final int BUTTON_X = 8 + ListData.WIDTH * 18 - 3 * 11;

    public GuiList(ContainerList menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/list_new.png"), 176, 191);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
        titleLabelY = 12;
    }

    private static int buttonY(int line) {
        return 32 + line * 34 + 18;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        for (int line = 0; line < ListData.HEIGHT; line++) {
            if (menu.isOneStackMode(line)) {
                blitPart(graphics, 6, 30 + line * 34, 0, 191, 20, 20);
                for (int i = 1; i < ListData.WIDTH; i++) {
                    blitPart(graphics, 8 + i * 18, 32 + line * 34, 176, 0, 16, 16);
                }
            }
            for (int option = 0; option < 3; option++) {
                int x = BUTTON_X + option * 11, y = buttonY(line);
                boolean on = menu.getOption(line, option);
                graphics.fill(leftPos + x, topPos + y, leftPos + x + 10, topPos + y + 10, on ? 0xFF55AA55 : 0xFF8B8B8B);
                blitPart(graphics, x + 1, y + 1, 176 + option * 9, 28, 8, 8);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        for (int line = 0; line < ListData.HEIGHT; line++) {
            for (int option = 0; option < 3; option++) {
                if (isHovering(BUTTON_X + option * 11, buttonY(line), 10, 10, mouseX, mouseY)) {
                    String state = menu.getOption(line, option) ? "on" : "off";
                    showTooltip(graphics, List.of(Component.translatable("gui.buildcraft.list." + OPTIONS[option] + "." + state)),
                        mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int line = 0; line < ListData.HEIGHT; line++) {
            for (int option = 0; option < 3; option++) {
                if (isHovering(BUTTON_X + option * 11, buttonY(line), 10, 10, event.x(), event.y())) {
                    sendButtonClick(line * 3 + option);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
