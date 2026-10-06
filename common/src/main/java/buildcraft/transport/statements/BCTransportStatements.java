/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.resources.Identifier;
import buildcraft.BuildCraft;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.pipe.behaviour.IColouredPipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLimiter;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
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
    public static final Map<DyeColor, IActionInternal> PIPE_COLOUR = new EnumMap<>(DyeColor.class);
    public static final Map<PipeBehaviourEmzuli.SlotIndex, IActionInternal> EXTRACTION_PRESET = new EnumMap<>(PipeBehaviourEmzuli.SlotIndex.class);
    /** The power limits of iron and diamond power pipes, by pipe id and then by limit shift. */
    public static final Map<String, IActionInternal[]> POWER_LIMIT = new java.util.LinkedHashMap<>();
    public static final ITriggerInternal POWER_REQUESTED = StatementManager.registerStatement(new PowerRequestedTrigger());
    private static final String[] LIMIT_ICONS = { "m256", "m128", "m64", "m16", "m8", "m2", "m0" };

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
        for (DyeColor colour : DyeColor.values()) {
            PIPE_COLOUR.put(colour, StatementManager.registerStatement(new PipeColourAction(colour)));
        }
        for (PipeBehaviourEmzuli.SlotIndex index : PipeBehaviourEmzuli.SlotIndex.VALUES) {
            EXTRACTION_PRESET.put(index, StatementManager.registerStatement(new ExtractionPresetAction(index)));
        }
        for (String pipe : new String[] { "iron_power", "diamond_power" }) {
            IActionInternal[] actions = new IActionInternal[PipeBehaviourLimiter.MAX_SHIFT + 1];
            for (int shift = 0; shift <= PipeBehaviourLimiter.MAX_SHIFT; shift++) {
                actions[shift] = StatementManager.registerStatement(new PowerLimitAction(pipe, shift));
            }
            POWER_LIMIT.put(pipe, actions);
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
                if (flow instanceof PipeFlowPower) {
                    triggers.add(PIPE_CONTENTS.get(Contents.ENERGY));
                    triggers.add(POWER_REQUESTED);
                }
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
                var behaviour = holder.getPipe().getBehaviour();
                if (behaviour instanceof IColouredPipe) actions.addAll(PIPE_COLOUR.values());
                if (behaviour instanceof PipeBehaviourEmzuli) actions.addAll(EXTRACTION_PRESET.values());
                IActionInternal[] limits = POWER_LIMIT.get(holder.getPipe().getDefinition().id);
                if (limits != null && behaviour instanceof PipeBehaviourLimiter) actions.addAll(java.util.List.of(limits));
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

    static class PipeColourAction extends BCStatement implements IActionInternal {
        private final DyeColor colour;
        private final Identifier icon;

        PipeColourAction(DyeColor colour) {
            super("pipe.colour." + colour.getSerializedName(), "gate.buildcraft.action.pipe.colour", "");
            this.colour = colour;
            this.icon = BuildCraft.id("textures/item/paintbrush/" + colour.getSerializedName() + ".png");
        }

        @Override
        public Identifier getIcon() {
            return icon;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.action.pipe.colour", Component.translatable("color.minecraft." + colour.getSerializedName()));
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate && gate.getPipeHolder().getPipe().getBehaviour() instanceof IColouredPipe coloured) {
                coloured.setColour(colour);
            }
        }
    }

    static class ExtractionPresetAction extends BCStatement implements IActionInternal {
        private final PipeBehaviourEmzuli.SlotIndex index;

        ExtractionPresetAction(PipeBehaviourEmzuli.SlotIndex index) {
            super("extraction.preset." + index.colour.getSerializedName(), "gate.buildcraft.action.extraction",
                "extraction_preset_" + index.colour.getSerializedName());
            this.index = index;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.action.extraction",
                Component.translatable("color.minecraft." + index.colour.getSerializedName()));
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate && gate.getPipeHolder().getPipe().getBehaviour() instanceof PipeBehaviourEmzuli emzuli) {
                emzuli.activate(index);
            }
        }
    }

    static class PowerLimitAction extends BCStatement implements IActionInternal {
        private final String pipe;
        private final int shift;

        PowerLimitAction(String pipe, int shift) {
            super("pipe.power_limit." + pipe + ".s" + shift, "gate.buildcraft.action.pipe.power_limit", "trigger_limiter_" + LIMIT_ICONS[shift]);
            this.pipe = pipe;
            this.shift = shift;
        }

        @Override
        public Component getDescription() {
            long limit = 0;
            PipeDefinition def = BCTransportPipes.def(pipe);
            if (def != null && shift < PipeBehaviourLimiter.MAX_SHIFT) {
                limit = (PipeApi.getPowerTransferInfo(def).transferPerTick() >> shift) / MjAPI.MJ;
            }
            return Component.translatable("gate.buildcraft.action.pipe.power_limit", limit);
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate && gate.getPipeHolder().getPipe().getBehaviour() instanceof PipeBehaviourLimiter limiter) {
                limiter.setLimitShift(shift);
            }
        }
    }

    static class PowerRequestedTrigger extends BCStatement implements ITriggerInternal {
        PowerRequestedTrigger() {
            super("pipe.requestsEnergy", "gate.buildcraft.trigger.pipe.requestsEnergy", "trigger_pipecontents_requestsenergy");
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            return source instanceof IGate gate && gate.getPipeHolder().getPipe().getFlow() instanceof PipeFlowPower power
                && power.getPowerRequested(null) > 0;
        }
    }
}
