/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.plug.PluggableGate;

/** The gate GUI. Changes are sent to the server as menu button clicks, with the details packed into the button id. */
public class ContainerGate extends ContainerBC<BlockEntity> {
    public static final int OP_SET_TRIGGER = 0, OP_SET_ACTION = 1, OP_TRIGGER_PARAM = 2, OP_ACTION_PARAM = 3, OP_CONNECTION = 4;
    /** The statement value that clears a slot. */
    public static final int CLEAR = 0xFFF;
    public static final int MAX_ROWS = 4;

    private final MenuData.Field posX, posY, posZ, side;
    @Nullable
    private PluggableGate serverGate;

    /** Client constructor. */
    public ContainerGate(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerGate(int id, Inventory inventory, @Nullable PluggableGate gate) {
        super(BCSiliconMenus.GATE.get(), id, inventory, null);
        this.serverGate = gate;
        posX = data.addInt(gate == null ? null : () -> gate.holder.getPipePos().getX());
        posY = data.addInt(gate == null ? null : () -> gate.holder.getPipePos().getY());
        posZ = data.addInt(gate == null ? null : () -> gate.holder.getPipePos().getZ());
        side = data.addInt(gate == null ? null : () -> gate.side.ordinal());
        // The client doesn't know the gate's size when the menu opens, so the GUI always has room for the largest gate
        addPlayerInventory(33 + MAX_ROWS * 18);
    }

    public static int getSlotRows(GateLogic logic) {
        int slots = logic.variant.numSlots();
        return logic.isSplitInTwo() ? (int) Math.ceil(slots / 2.0) : slots;
    }

    /** @return The gate, or null on the client until the server has said where it is. */
    @Nullable
    public PluggableGate getGate() {
        if (serverGate != null) return serverGate;
        BlockPos pos = new BlockPos(posX.getInt(), posY.getInt(), posZ.getInt());
        int s = side.getInt();
        if (s < 0 || s >= 6) return null;
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof IPipeHolder holder && holder.getPluggable(Direction.values()[s]) instanceof PluggableGate gate) {
            return gate;
        }
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        PluggableGate gate = getGate();
        if (gate == null) return serverGate == null;
        BlockPos pos = gate.holder.getPipePos();
        return gate.holder.getPluggable(gate.side) == gate && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }

    public static int encode(int op, int slot, int param, int value) {
        return (op << 24) | (slot << 16) | (param << 12) | (value & 0xFFF);
    }

    /** @return The value to send for choosing a statement. */
    public static int encodeOption(GateLogic.Option option) {
        int index = StatementManager.getStatementIndex(option.statement());
        return index * 7 + (option.side() == null ? 6 : option.side().ordinal());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        PluggableGate gate = serverGate;
        if (gate == null) return false;
        GateLogic logic = gate.logic;
        int op = id >>> 24;
        int slot = (id >>> 16) & 0xFF;
        int param = (id >>> 12) & 0xF;
        int value = id & 0xFFF;
        if (op == OP_CONNECTION) {
            if (slot < logic.connections.length) {
                logic.connections[slot] = !logic.connections[slot];
                gate.scheduleNetworkUpdate();
            }
            return true;
        }
        if (slot >= logic.triggers.length) return false;
        boolean isAction = op == OP_SET_ACTION || op == OP_ACTION_PARAM;
        StatementWrapper[] slots = isAction ? logic.actions : logic.triggers;
        if (op == OP_SET_TRIGGER || op == OP_SET_ACTION) {
            if (value == CLEAR) {
                slots[slot] = null;
            } else {
                IStatement statement = StatementManager.getStatementByIndex(value / 7);
                int sideIndex = value % 7;
                Direction dir = sideIndex == 6 ? null : Direction.values()[sideIndex];
                if (statement == null) return false;
                GateLogic.Option option = new GateLogic.Option(statement, dir);
                if (!(isAction ? logic.getPossibleActions() : logic.getPossibleTriggers()).contains(option)) {
                    return false;
                }
                slots[slot] = StatementWrapper.create(statement, dir, isAction ? logic.variant.numActionParams()
                    : logic.variant.numTriggerParams());
            }
            gate.scheduleNetworkUpdate();
            return true;
        }
        if (op == OP_TRIGGER_PARAM || op == OP_ACTION_PARAM) {
            StatementWrapper wrapper = slots[slot];
            if (wrapper == null || param >= wrapper.parameters.length) return false;
            IStatementParameter current = wrapper.parameters[param];
            if (current == null) return false;
            wrapper.parameters[param] = current.onClick(logic, wrapper.statement, getCarried(), value);
            gate.scheduleNetworkUpdate();
            return true;
        }
        return false;
    }
}
