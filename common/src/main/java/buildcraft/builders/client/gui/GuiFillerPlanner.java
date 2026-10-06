/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.addon.AddonFillerPlanner;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.container.ContainerFillerPlanner;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.lib.gui.GuiBC;

/** The filler planner's GUI: the pattern (click it to pick another), its parameters, and a button to invert it. */
public class GuiFillerPlanner extends GuiBC<ContainerFillerPlanner> {
    private static final int PATTERN_X = 12, PATTERN_Y = 32;
    private static final int PARAM_X = 53, PARAM_Y = 39;
    private static final int INVERT_X = 152, BUTTON_Y = 40;
    private static final int OPTION_COLUMNS = 8;

    private boolean choosing = false;

    public GuiFillerPlanner(ContainerFillerPlanner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/filler_planner.png"), 176, 81);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 8;
        inventoryLabelY = -1000;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        Pattern pattern = menu.getPattern();
        graphics.blit(RenderPipelines.GUI_TEXTURED, pattern.getIcon(), leftPos + PATTERN_X, topPos + PATTERN_Y, 0, 0, 32, 32, 16, 16,
            16, 16);
        for (int i = 0; i < AddonFillerPlanner.PARAM_COUNT; i++) {
            if (i >= pattern.maxParameters()) {
                graphics.fill(leftPos + PARAM_X + 18 * i, topPos + PARAM_Y, leftPos + PARAM_X + 18 * i + 18, topPos + PARAM_Y + 18,
                    0x80000000);
                continue;
            }
            IStatementParameter param = menu.getParam(i);
            if (param != null && param.getIcon() != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, param.getIcon(), leftPos + PARAM_X + 18 * i + 1, topPos + PARAM_Y + 1, 0, 0,
                    16, 16, 16, 16);
            }
        }
        boolean hovered = isHovering(INVERT_X, BUTTON_Y, 16, 16, mouseX, mouseY);
        blitPart(graphics, INVERT_X, BUTTON_Y, 224 + (menu.inverted.getBoolean() ? 16 : 0), hovered ? 16 : 0, 16, 16);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (choosing) {
            int x0 = leftPos + optionsX(), y0 = topPos + optionsY();
            int rows = (Patterns.ALL.size() + OPTION_COLUMNS - 1) / OPTION_COLUMNS;
            graphics.fill(x0 - 2, y0 - 2, x0 + OPTION_COLUMNS * 18 + 2, y0 + rows * 18 + 2, 0xF0100010);
            for (int i = 0; i < Patterns.ALL.size(); i++) {
                int x = x0 + (i % OPTION_COLUMNS) * 18, y = y0 + (i / OPTION_COLUMNS) * 18;
                graphics.blit(RenderPipelines.GUI_TEXTURED, Patterns.ALL.get(i).getIcon(), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    graphics.fill(x, y, x + 18, y + 18, 0x40FFFFFF);
                    showTooltip(graphics, List.of(Patterns.ALL.get(i).getDescription()), mouseX, mouseY);
                }
            }
            return;
        }
        List<Component> tooltip = new ArrayList<>();
        Pattern pattern = menu.getPattern();
        if (isHovering(PATTERN_X, PATTERN_Y, 32, 32, mouseX, mouseY)) {
            tooltip.add(pattern.getDescription());
        }
        for (int i = 0; i < Math.min(pattern.maxParameters(), AddonFillerPlanner.PARAM_COUNT); i++) {
            IStatementParameter param = menu.getParam(i);
            if (param != null && isHovering(PARAM_X + 18 * i, PARAM_Y, 18, 18, mouseX, mouseY)) {
                tooltip.add(param.getDescription());
            }
        }
        if (isHovering(INVERT_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            tooltip.add(Component.translatable("tip.filler.invert." + (menu.inverted.getBoolean() ? "on" : "off")));
        }
        if (!tooltip.isEmpty()) {
            showTooltip(graphics, tooltip, mouseX, mouseY);
        }
    }

    private int optionsX() {
        return (imageWidth - OPTION_COLUMNS * 18) / 2;
    }

    private int optionsY() {
        return PATTERN_Y + 36;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (choosing) {
            choosing = false;
            int x0 = leftPos + optionsX(), y0 = topPos + optionsY();
            for (int i = 0; i < Patterns.ALL.size(); i++) {
                int x = x0 + (i % OPTION_COLUMNS) * 18, y = y0 + (i / OPTION_COLUMNS) * 18;
                if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                    sendButtonClick(ContainerFiller.BUTTON_PATTERN + i);
                    return true;
                }
            }
            return true;
        }
        if (isHovering(PATTERN_X, PATTERN_Y, 32, 32, mx, my)) {
            choosing = true;
            return true;
        }
        for (int i = 0; i < Math.min(menu.getPattern().maxParameters(), AddonFillerPlanner.PARAM_COUNT); i++) {
            if (isHovering(PARAM_X + 18 * i, PARAM_Y, 18, 18, mx, my)) {
                sendButtonClick(ContainerFiller.BUTTON_PARAM + i * 2 + containerButton(event));
                return true;
            }
        }
        if (isHovering(INVERT_X, BUTTON_Y, 16, 16, mx, my)) {
            sendButtonClick(ContainerFiller.BUTTON_INVERT);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
