/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.silicon.plug.PluggableGate;

/** The triggers, actions and logic of a gate. Each slot pairs a trigger with an action; slots can be connected so that
 * a group of triggers controls a group of actions. */
public class GateLogic implements IGate {
    public final PluggableGate pluggable;
    public final GateVariant variant;
    public final @Nullable StatementWrapper[] triggers;
    public final @Nullable StatementWrapper[] actions;
    /** connections[i] joins slot i with slot i + 1. */
    public final boolean[] connections;
    public final boolean[] triggerOn, actionOn;
    public boolean isOn;
    private int redstoneOutput = 0;
    private boolean redstoneSideOnly = false;
    private final Set<DyeColor> wireBroadcasts = EnumSet.noneOf(DyeColor.class);

    public GateLogic(PluggableGate pluggable, GateVariant variant) {
        this.pluggable = pluggable;
        this.variant = variant;
        int slots = variant.numSlots();
        triggers = new StatementWrapper[slots];
        actions = new StatementWrapper[slots];
        connections = new boolean[Math.max(0, slots - 1)];
        triggerOn = new boolean[slots];
        actionOn = new boolean[slots];
    }

    // Saving

    public void save(ValueOutput output) {
        int c = 0;
        for (int i = 0; i < connections.length; i++) {
            if (connections[i]) c |= 1 << i;
        }
        output.putInt("connections", c);
        int on = 0;
        for (int i = 0; i < triggers.length; i++) {
            if (triggerOn[i]) on |= 1 << i;
            if (actionOn[i]) on |= 1 << (i + 16);
        }
        output.putInt("on", on);
        output.putBoolean("isOn", isOn);
        for (int i = 0; i < triggers.length; i++) {
            if (triggers[i] != null) triggers[i].save(output.child("trigger" + i));
            if (actions[i] != null) actions[i].save(output.child("action" + i));
        }
        int wires = 0;
        for (DyeColor colour : wireBroadcasts) wires |= 1 << colour.ordinal();
        output.putInt("wires", wires);
    }

    public void load(ValueInput input) {
        int c = input.getIntOr("connections", 0);
        for (int i = 0; i < connections.length; i++) {
            connections[i] = ((c >>> i) & 1) == 1;
        }
        int on = input.getIntOr("on", 0);
        for (int i = 0; i < triggers.length; i++) {
            triggerOn[i] = ((on >>> i) & 1) == 1;
            actionOn[i] = ((on >>> (i + 16)) & 1) == 1;
            triggers[i] = input.child("trigger" + i).map(t -> StatementWrapper.load(t, variant.numTriggerParams())).orElse(null);
            actions[i] = input.child("action" + i).map(a -> StatementWrapper.load(a, variant.numActionParams())).orElse(null);
        }
        isOn = input.getBooleanOr("isOn", false);
        wireBroadcasts.clear();
        int wires = input.getIntOr("wires", 0);
        for (DyeColor colour : DyeColor.values()) {
            if (((wires >>> colour.ordinal()) & 1) == 1) wireBroadcasts.add(colour);
        }
    }

    // IGate

    @Override
    public Direction getSide() {
        return pluggable.side;
    }

    @Override
    public IPipeHolder getPipeHolder() {
        return pluggable.holder;
    }

    @Override
    public Level getLevel() {
        return pluggable.holder.getPipeWorld();
    }

    @Override
    public BlockPos getPos() {
        return pluggable.holder.getPipePos();
    }

    @Override
    public BlockEntity getTile() {
        return getLevel().getBlockEntity(getPos());
    }

    @Override
    public @Nullable BlockEntity getNeighbourTile(Direction side) {
        return pluggable.holder.getNeighbourTile(side);
    }

    @Override
    public int getRedstoneInput() {
        return getLevel().getBestNeighborSignal(getPos());
    }

    @Override
    public void setRedstoneOutput(int level, boolean sideOnly) {
        if (level > redstoneOutput || (level == redstoneOutput && !sideOnly)) {
            redstoneOutput = level;
            redstoneSideOnly = sideOnly;
        }
    }

    @Override
    public void emitWire(DyeColor colour) {
        wireBroadcasts.add(colour);
    }

    public boolean isEmitting(DyeColor colour) {
        return wireBroadcasts.contains(colour);
    }

    public Set<DyeColor> getWireBroadcasts() {
        return wireBroadcasts;
    }

    /** @return The redstone signal this gate gives out of the given side of the pipe. */
    public int getRedstoneOutput(Direction pipeSide) {
        return redstoneSideOnly && pipeSide != getSide() ? 0 : redstoneOutput;
    }

    // Logic

    /** Works out which triggers are on, and runs the actions they control. Called every tick on the server.
     * @return True if anything the client shows changed. */
    public boolean resolveActions() {
        boolean prevIsOn = isOn;
        boolean[] prevTriggers = triggerOn.clone();
        boolean[] prevActions = actionOn.clone();
        int prevRedstone = redstoneOutput;
        boolean prevSideOnly = redstoneSideOnly;
        Set<DyeColor> prevWires = EnumSet.noneOf(DyeColor.class);
        prevWires.addAll(wireBroadcasts);
        isOn = false;
        Arrays.fill(triggerOn, false);
        Arrays.fill(actionOn, false);
        redstoneOutput = 0;
        redstoneSideOnly = false;
        wireBroadcasts.clear();

        int groupCount = 0;
        int groupActive = 0;
        for (int i = 0; i < triggers.length; i++) {
            groupCount++;
            StatementWrapper trigger = triggers[i];
            if (trigger != null && trigger.isTriggerActive(this)) {
                groupActive++;
                triggerOn[i] = true;
            }
            if (i == connections.length || !connections[i]) {
                boolean active = variant.logic() == GateVariant.Logic.AND ? groupActive == groupCount : groupActive > 0;
                for (int j = i - groupCount + 1; j <= i; j++) {
                    StatementWrapper action = actions[j];
                    actionOn[j] = active;
                    if (action == null) continue;
                    if (active) {
                        isOn = true;
                        action.activate(this);
                    } else {
                        action.deactivate(this);
                    }
                }
                groupCount = 0;
                groupActive = 0;
            }
        }
        return isOn != prevIsOn || !Arrays.equals(prevTriggers, triggerOn) || !Arrays.equals(prevActions, actionOn)
            || prevRedstone != redstoneOutput || prevSideOnly != redstoneSideOnly || !prevWires.equals(wireBroadcasts);
    }

    public boolean didRedstoneChange(int prev) {
        return prev != redstoneOutput;
    }

    // Choosing statements

    /** A statement that could be put in a slot, along with the side it looks at. */
    public record Option(IStatement statement, @Nullable Direction side) {}

    public Set<Option> getPossibleTriggers() {
        Set<Option> set = new LinkedHashSet<>();
        StatementManager.getInternalTriggers(this).forEach(t -> addIfValid(set, t, null, variant.numTriggerParams()));
        for (Direction side : Direction.values()) {
            StatementManager.getInternalSidedTriggers(this, side).forEach(t -> addIfValid(set, t, side, variant.numTriggerParams()));
            BlockEntity tile = getNeighbourTile(side);
            if (tile != null) {
                StatementManager.getExternalTriggers(side.getOpposite(), tile).forEach(t -> addIfValid(set, t, side, variant.numTriggerParams()));
            }
        }
        return set;
    }

    public Set<Option> getPossibleActions() {
        Set<Option> set = new LinkedHashSet<>();
        StatementManager.getInternalActions(this).forEach(a -> addIfValid(set, a, null, variant.numActionParams()));
        for (Direction side : Direction.values()) {
            StatementManager.getInternalSidedActions(this, side).forEach(a -> addIfValid(set, a, side, variant.numActionParams()));
            BlockEntity tile = getNeighbourTile(side);
            if (tile != null) {
                StatementManager.getExternalActions(side.getOpposite(), tile).forEach(a -> addIfValid(set, a, side, variant.numActionParams()));
            }
        }
        return set;
    }

    private static void addIfValid(Set<Option> set, IStatement statement, @Nullable Direction side, int params) {
        if (statement.minParameters() <= params) {
            set.add(new Option(statement, side));
        }
    }

    public boolean isSplitInTwo() {
        return triggers.length > 4;
    }
}
