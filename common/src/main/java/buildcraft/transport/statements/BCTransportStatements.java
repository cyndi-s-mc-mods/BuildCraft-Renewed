/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.IStatementProvider;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.lib.statement.BCStatement;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDirectional;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.wire.WireManager;

/** Triggers and actions for pipes: what's in the pipe, pipe wire signals, and which way a pipe points. */
public final class BCTransportStatements {
    private BCTransportStatements() {}

    public enum Contents {
        EMPTY("empty", "trigger_pipecontents_empty"),
        ITEMS("containsItems", "trigger_pipecontents_containsitems"),
        FLUIDS("containsFluids", "trigger_pipecontents_containsfluids"),
        ENERGY("containsEnergy", "trigger_pipecontents_containsenergy");

        final String name, icon;

        Contents(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    public static final Map<Contents, ITriggerInternal> PIPE_CONTENTS = new EnumMap<>(Contents.class);
    public static final Map<DyeColor, ITriggerInternal> SIGNAL_ON = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, ITriggerInternal> SIGNAL_OFF = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, IActionInternal> SIGNAL_EMIT = new EnumMap<>(DyeColor.class);
    public static final Map<Direction, IActionInternal> PIPE_DIRECTION = new EnumMap<>(Direction.class);

    static {
        for (Contents contents : Contents.values()) {
            PIPE_CONTENTS.put(contents, StatementManager.registerStatement(new PipeContentsTrigger(contents)));
        }
        for (DyeColor colour : DyeColor.values()) {
            SIGNAL_ON.put(colour, StatementManager.registerStatement(new PipeSignalTrigger(colour, true)));
            SIGNAL_OFF.put(colour, StatementManager.registerStatement(new PipeSignalTrigger(colour, false)));
            SIGNAL_EMIT.put(colour, StatementManager.registerStatement(new PipeSignalAction(colour)));
        }
        for (Direction dir : Direction.values()) {
            PIPE_DIRECTION.put(dir, StatementManager.registerStatement(new PipeDirectionAction(dir)));
        }
    }

    public static void init() {
        StatementManager.registerProvider(new IStatementProvider() {
            @Override
            public void addInternalTriggers(Collection<ITriggerInternal> triggers, IStatementContainer container) {
                if (!(container instanceof IGate gate)) return;
                IPipeHolder holder = gate.getPipeHolder();
                var flow = holder.getPipe().getFlow();
                triggers.add(PIPE_CONTENTS.get(Contents.EMPTY));
                if (flow instanceof PipeFlowItems) triggers.add(PIPE_CONTENTS.get(Contents.ITEMS));
                if (flow instanceof PipeFlowFluids) triggers.add(PIPE_CONTENTS.get(Contents.FLUIDS));
                if (flow instanceof PipeFlowPower) triggers.add(PIPE_CONTENTS.get(Contents.ENERGY));
                for (DyeColor colour : holder.getWires().values()) {
                    triggers.add(SIGNAL_ON.get(colour));
                    triggers.add(SIGNAL_OFF.get(colour));
                }
            }

            @Override
            public void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {
                if (!(container instanceof IGate gate)) return;
                IPipeHolder holder = gate.getPipeHolder();
                for (DyeColor colour : holder.getWires().values()) {
                    actions.add(SIGNAL_EMIT.get(colour));
                }
                if (holder.getPipe().getBehaviour() instanceof PipeBehaviourDirectional) {
                    for (Direction dir : Direction.values()) {
                        if (holder.getPipe().isConnected(dir)) actions.add(PIPE_DIRECTION.get(dir));
                    }
                }
            }
        });
    }

    static class PipeContentsTrigger extends BCStatement implements ITriggerInternal {
        private final Contents contents;

        PipeContentsTrigger(Contents contents) {
            super("pipe." + contents.name, "gate.buildcraft.trigger.pipe." + contents.name, contents.icon);
            this.contents = contents;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            if (!(source instanceof IGate gate)) return false;
            var flow = gate.getPipeHolder().getPipe().getFlow();
            boolean items = flow instanceof PipeFlowItems f && f.hasItems();
            boolean fluids = flow instanceof PipeFlowFluids f && f.getTotalAmount() > 0;
            boolean energy = flow instanceof PipeFlowPower f && f.hasPower();
            return switch (contents) {
                case EMPTY -> !items && !fluids && !energy;
                case ITEMS -> items;
                case FLUIDS -> fluids;
                case ENERGY -> energy;
            };
        }
    }

    static class PipeSignalTrigger extends BCStatement implements ITriggerInternal {
        private final DyeColor colour;
        private final boolean active;

        PipeSignalTrigger(DyeColor colour, boolean active) {
            super("pipe.wire." + colour.getSerializedName() + (active ? ".active" : ".inactive"),
                "gate.buildcraft.trigger.pipe.wire." + (active ? "active" : "inactive"),
                "trigger_pipesignal_" + iconColour(colour) + (active ? "_active" : "_inactive"));
            this.colour = colour;
            this.active = active;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.trigger.pipe.wire." + (active ? "active" : "inactive"),
                Component.translatable("color.minecraft." + colour.getSerializedName()));
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            if (!(source instanceof IGate gate)) return false;
            boolean powered = false;
            for (Map.Entry<EnumWirePart, DyeColor> wire : gate.getPipeHolder().getWires().entrySet()) {
                if (wire.getValue() == colour && WireManager.isPowered(gate.getLevel(), gate.getPos(), wire.getKey())) {
                    powered = true;
                    break;
                }
            }
            return powered == active;
        }
    }

    static String iconColour(DyeColor colour) {
        return colour == DyeColor.LIGHT_GRAY ? "silver" : colour.getSerializedName();
    }

    static class PipeSignalAction extends BCStatement implements IActionInternal {
        private final DyeColor colour;

        PipeSignalAction(DyeColor colour) {
            super("pipe.wire.output." + colour.getSerializedName(), "gate.buildcraft.action.pipe.wire",
                "trigger_pipesignal_" + iconColour(colour) + "_active");
            this.colour = colour;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.action.pipe.wire", Component.translatable("color.minecraft." + colour.getSerializedName()));
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate) {
                gate.emitWire(colour);
            }
        }
    }

    static class PipeDirectionAction extends BCStatement implements IActionInternal {
        private final Direction dir;

        PipeDirectionAction(Direction dir) {
            super("pipe.dir." + dir.getSerializedName(), "gate.buildcraft.action.pipe.direction", "trigger_dir_" + dir.getSerializedName());
            this.dir = dir;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.action.pipe.direction", Component.translatable("direction.buildcraft." + dir.getSerializedName()));
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate && gate.getPipeHolder().getPipe().getBehaviour() instanceof PipeBehaviourDirectional directional) {
                directional.setDirectionIfValid(dir);
            }
        }
    }
}
