/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.lib.gui.GuiBC;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.plug.PluggableGate;

/** The gate GUI: trigger and action slots, with their parameters, and the connections between them. Clicking a slot
 * shows the statements that can go in it. */
public class GuiGate extends GuiBC<ContainerGate> {
    private static final int ROWS_HEIGHT = ContainerGate.MAX_ROWS * 18;
    private static final int OPTION_COLUMNS = 8;

    /** The slot whose options are being shown, or -1. */
    private int selectingSlot = -1;
    private boolean selectingAction;
    private final List<GateLogic.Option> options = new ArrayList<>();

    public GuiGate(ContainerGate menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/gate_interface.png"), 176, 16 + 101 + ROWS_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 16 + ROWS_HEIGHT + 4;
    }

    @Nullable
    private GateLogic logic() {
        PluggableGate gate = menu.getGate();
        return gate == null ? null : gate.logic;
    }

    // Layout

    private int columns(GateLogic logic) {
        return logic.isSplitInTwo() ? 2 : 1;
    }

    private int rows(GateLogic logic) {
        return ContainerGate.getSlotRows(logic);
    }

    private int pairWidth(GateLogic logic) {
        return 18 * (3 + logic.variant.numTriggerParams() + logic.variant.numActionParams());
    }

    private int pairStart(GateLogic logic) {
        int width = pairWidth(logic);
        return (162 - (width + (logic.isSplitInTwo() ? width + 18 : 0))) / 2;
    }

    /** @return The position (relative to the GUI) of the trigger slot of a pair. */
    private int pairX(GateLogic logic, int index) {
        int column = index / rows(logic);
        return pairStart(logic) + 7 + column * (18 + pairWidth(logic));
    }

    private int pairY(GateLogic logic, int index) {
        return 16 + (index % rows(logic)) * 18;
    }

    private int actionX(GateLogic logic, int index) {
        return pairX(logic, index) + 18 * (2 + logic.variant.numTriggerParams());
    }

    // Drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        // The background is made from parts, so don't use the default full image
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0, 0, 176, 16, 256, 256);
        for (int r = 0; r < ContainerGate.MAX_ROWS; r++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos + 16 + r * 18, 0, 23, 176, 18, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos + 16 + ROWS_HEIGHT, 0, 48, 176, 101, 256, 256);
        GateLogic logic = logic();
        if (logic != null) {
            drawGate(graphics, logic);
        }
    }

    private void slotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        blitPart(graphics, x, y, 7, 64, 18, 18);
    }

    private void icon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, leftPos + x + 1, topPos + y + 1, 0, 0, 16, 16, 16, 16);
    }

    private void drawGate(GuiGraphicsExtractor graphics, GateLogic logic) {
        int slots = logic.triggers.length;
        int trigParams = logic.variant.numTriggerParams();
        int actParams = logic.variant.numActionParams();
        for (int i = 0; i < slots; i++) {
            int x = pairX(logic, i), y = pairY(logic, i);
            slotFrame(graphics, x, y);
            drawStatement(graphics, logic.triggers[i], x, y);
            for (int p = 0; p < trigParams; p++) {
                slotFrame(graphics, x + 18 * (p + 1), y);
                drawParam(graphics, logic.triggers[i], p, x + 18 * (p + 1), y);
            }
            int connector = x + 18 * (1 + trigParams);
            boolean actionActive = logic.actionOn[i] && logic.actions[i] != null;
            blitPart(graphics, connector, y, 176 + (logic.triggerOn[i] ? 18 : 0), 18, 7, 18);
            blitPart(graphics, connector + 7, y, 180 + (logic.actionOn[i] ? 18 : 0), 18, 4, 18);
            blitPart(graphics, connector + 11, y, 187 + (actionActive ? 18 : 0), 18, 7, 18);
            int ax = actionX(logic, i);
            slotFrame(graphics, ax, y);
            drawStatement(graphics, logic.actions[i], ax, y);
            for (int p = 0; p < actParams; p++) {
                slotFrame(graphics, ax + 18 * (p + 1), y);
                drawParam(graphics, logic.actions[i], p, ax + 18 * (p + 1), y);
            }
        }
        // Connections between pairs in the same column
        for (int i = 0; i < logic.connections.length; i++) {
            if ((i + 1) % rows(logic) == 0) continue;
            int x = pairX(logic, i) + 18 * (1 + trigParams), y = pairY(logic, i) + 9;
            boolean connected = logic.connections[i];
            blitPart(graphics, x, y, 176 + (logic.actionOn[i] ? 18 : 0), 36 + (connected ? 18 : 0), 18, 9);
            blitPart(graphics, x, y + 9, 176 + (logic.actionOn[i + 1] ? 18 : 0), 45 + (connected ? 18 : 0), 18, 9);
        }
    }

    private void drawStatement(GuiGraphicsExtractor graphics, @Nullable StatementWrapper wrapper, int x, int y) {
        if (wrapper != null) {
            icon(graphics, wrapper.statement.getIcon(), x, y);
        }
    }

    private void drawParam(GuiGraphicsExtractor graphics, @Nullable StatementWrapper wrapper, int index, int x, int y) {
        if (wrapper == null || index >= wrapper.parameters.length) return;
        IStatementParameter param = wrapper.parameters[index];
        if (param == null) return;
        Identifier paramIcon = param.getIcon();
        if (paramIcon != null) {
            icon(graphics, paramIcon, x, y);
        }
        ItemStack stack = param.getItemStack();
        if (!stack.isEmpty()) {
            graphics.item(stack, leftPos + x + 1, topPos + y + 1);
        }
    }

    // The list of statements to choose from

    private int optionsX() {
        return -2 - OPTION_COLUMNS * 18 + 176 / 2 + OPTION_COLUMNS * 9;
    }

    private int optionsY(GateLogic logic) {
        return pairY(logic, selectingSlot) + 20;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        GateLogic logic = logic();
        if (logic == null || selectingSlot < 0) return;
        int x0 = leftPos + optionsX(), y0 = topPos + optionsY(logic);
        int rowsNeeded = Math.max(1, (options.size() + OPTION_COLUMNS - 1) / OPTION_COLUMNS);
        graphics.fill(x0 - 2, y0 - 2, x0 + OPTION_COLUMNS * 18 + 2, y0 + rowsNeeded * 18 + 2, 0xF0100010);
        for (int i = 0; i < options.size(); i++) {
            int x = x0 + (i % OPTION_COLUMNS) * 18, y = y0 + (i / OPTION_COLUMNS) * 18;
            graphics.blit(RenderPipelines.GUI_TEXTURED, options.get(i).statement().getIcon(), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                graphics.fill(x, y, x + 18, y + 18, 0x40FFFFFF);
                GateLogic.Option option = options.get(i);
                StatementWrapper preview = new StatementWrapper(option.statement(), option.side(), new IStatementParameter[0]);
                showTooltip(graphics, List.of(preview.getDescription()), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        GateLogic logic = logic();
        if (logic == null || selectingSlot >= 0) return;
        for (int i = 0; i < logic.triggers.length; i++) {
            int x = pairX(logic, i), y = pairY(logic, i), ax = actionX(logic, i);
            tooltipFor(graphics, logic.triggers[i], logic.variant.numTriggerParams(), x, y, mouseX, mouseY);
            tooltipFor(graphics, logic.actions[i], logic.variant.numActionParams(), ax, y, mouseX, mouseY);
        }
    }

    private void tooltipFor(GuiGraphicsExtractor graphics, @Nullable StatementWrapper wrapper, int params, int x, int y, int mouseX,
        int mouseY) {
        if (wrapper == null) return;
        if (isHovering(x, y, 18, 18, mouseX, mouseY)) {
            showTooltip(graphics, List.of(wrapper.getDescription()), mouseX, mouseY);
        }
        for (int p = 0; p < Math.min(params, wrapper.parameters.length); p++) {
            IStatementParameter param = wrapper.parameters[p];
            if (param != null && isHovering(x + 18 * (p + 1), y, 18, 18, mouseX, mouseY)) {
                showTooltip(graphics, List.of(param.getDescription()), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        GateLogic logic = logic();
        if (logic == null) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (selectingSlot >= 0) {
            int x0 = leftPos + optionsX(), y0 = topPos + optionsY(logic);
            for (int i = 0; i < options.size(); i++) {
                int x = x0 + (i % OPTION_COLUMNS) * 18, y = y0 + (i / OPTION_COLUMNS) * 18;
                if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                    int op = selectingAction ? ContainerGate.OP_SET_ACTION : ContainerGate.OP_SET_TRIGGER;
                    sendButtonClick(ContainerGate.encode(op, selectingSlot, 0, ContainerGate.encodeOption(options.get(i))));
                    break;
                }
            }
            selectingSlot = -1;
            return true;
        }
        for (int i = 0; i < logic.triggers.length; i++) {
            int x = pairX(logic, i), y = pairY(logic, i), ax = actionX(logic, i);
            if (clickStatement(logic, i, false, x, y, mx, my, button) || clickStatement(logic, i, true, ax, y, mx, my, button)) {
                return true;
            }
        }
        for (int i = 0; i < logic.connections.length; i++) {
            if ((i + 1) % rows(logic) == 0) continue;
            int x = pairX(logic, i) + 18 * (1 + logic.variant.numTriggerParams()), y = pairY(logic, i) + 9;
            if (isHovering(x, y, 18, 18, mx, my)) {
                sendButtonClick(ContainerGate.encode(ContainerGate.OP_CONNECTION, i, 0, 0));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickStatement(GateLogic logic, int index, boolean action, int x, int y, double mx, double my, int button) {
        if (isHovering(x, y, 18, 18, mx, my)) {
            if (button == 1) {
                int op = action ? ContainerGate.OP_SET_ACTION : ContainerGate.OP_SET_TRIGGER;
                sendButtonClick(ContainerGate.encode(op, index, 0, ContainerGate.CLEAR));
            } else {
                selectingSlot = index;
                selectingAction = action;
                options.clear();
                options.addAll(action ? logic.getPossibleActions() : logic.getPossibleTriggers());
            }
            return true;
        }
        int params = action ? logic.variant.numActionParams() : logic.variant.numTriggerParams();
        for (int p = 0; p < params; p++) {
            if (isHovering(x + 18 * (p + 1), y, 18, 18, mx, my)) {
                int op = action ? ContainerGate.OP_ACTION_PARAM : ContainerGate.OP_TRIGGER_PARAM;
                sendButtonClick(ContainerGate.encode(op, index, p, button));
                return true;
            }
        }
        return false;
    }
}
