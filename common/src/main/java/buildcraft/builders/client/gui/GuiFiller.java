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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.tiles.IControllable;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.builders.tile.TileFiller;
import buildcraft.lib.gui.GuiBC;

/** The filler's GUI: the pattern and its parameters, buttons for excavating and inverting, and the block inventory.
 * Clicking the pattern shows every pattern to pick from. */
public class GuiFiller extends GuiBC<ContainerFiller> {
    private static final int PATTERN_X = 12, PATTERN_Y = 32;
    private static final int PARAM_X = 53, PARAM_Y = 39;
    private static final int EXCAVATE_X = 130, INVERT_X = 152, BUTTON_Y = 40;
    private static final int OPTION_COLUMNS = 8;
    private static final Identifier LOCK = BuildCraft.id("textures/gui/icons/lock.png");

    private boolean choosing = false;

    public GuiFiller(ContainerFiller menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/filler.png"), 176, 241);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        inventoryLabelY = 141;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        Pattern pattern = menu.getPattern();
        graphics.blit(RenderPipelines.GUI_TEXTURED, pattern.getIcon(), leftPos + PATTERN_X, topPos + PATTERN_Y, 0, 0, 32, 32, 16, 16,
            16, 16);
        for (int i = 0; i < TileFiller.PARAM_COUNT; i++) {
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
        drawButton(graphics, EXCAVATE_X, 192, menu.excavate.getBoolean(), mouseX, mouseY);
        drawButton(graphics, INVERT_X, 224, menu.inverted.getBoolean(), mouseX, mouseY);
        if (menu.locked.getBoolean()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, LOCK, leftPos + 12, topPos + 16, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int u, boolean active, int mouseX, int mouseY) {
        boolean hovered = isHovering(x, BUTTON_Y, 16, 16, mouseX, mouseY);
        blitPart(graphics, x, BUTTON_Y, u + (active ? 16 : 0), hovered ? 16 : 0, 16, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.buildcraft.filler.resources"), 8, 74, 0xFF404040, false);
        String status = !menu.hasBox.getBoolean() ? "gui.buildcraft.filler.no_area"
            : menu.finished.getBoolean() ? "gui.buildcraft.filler.finished"
            : menu.mode.getInt() == IControllable.Mode.OFF.ordinal() ? "gui.buildcraft.filler.off" : null;
        Component progress = status != null ? Component.translatable(status)
            : Component.translatable("gui.buildcraft.filler.progress", menu.toBreak.getInt(), menu.toPlace.getInt());
        graphics.text(font, progress, 53, 61, 0xFF404040, false);
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
            if (menu.locked.getBoolean()) tooltip.add(Component.translatable("gui.buildcraft.filler.locked"));
        }
        for (int i = 0; i < Math.min(pattern.maxParameters(), TileFiller.PARAM_COUNT); i++) {
            IStatementParameter param = menu.getParam(i);
            if (param != null && isHovering(PARAM_X + 18 * i, PARAM_Y, 18, 18, mouseX, mouseY)) {
                tooltip.add(param.getDescription());
            }
        }
        if (isHovering(EXCAVATE_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            tooltip.add(Component.translatable("tip.filler.excavate." + (menu.excavate.getBoolean() ? "on" : "off")));
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
        int button = containerButton(event);
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
            if (!menu.locked.getBoolean()) choosing = true;
            return true;
        }
        for (int i = 0; i < Math.min(menu.getPattern().maxParameters(), TileFiller.PARAM_COUNT); i++) {
            if (isHovering(PARAM_X + 18 * i, PARAM_Y, 18, 18, mx, my)) {
                sendButtonClick(ContainerFiller.BUTTON_PARAM + i * 2 + (button == 1 ? 1 : 0));
                return true;
            }
        }
        if (isHovering(EXCAVATE_X, BUTTON_Y, 16, 16, mx, my)) {
            sendButtonClick(ContainerFiller.BUTTON_EXCAVATE);
            return true;
        }
        if (isHovering(INVERT_X, BUTTON_Y, 16, 16, mx, my)) {
            sendButtonClick(ContainerFiller.BUTTON_INVERT);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
