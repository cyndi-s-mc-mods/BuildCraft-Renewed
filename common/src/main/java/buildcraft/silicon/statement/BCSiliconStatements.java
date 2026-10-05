/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.statement;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.IStatementProvider;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.statement.BCStatement;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.PluggablePulsar;

/** Statements that come from pluggables: the pulsar, light sensor and timer. */
public final class BCSiliconStatements {
    private BCSiliconStatements() {}

    public static final ITriggerInternalSided LIGHT_BRIGHT = StatementManager.registerStatement(new LightTrigger(true));
    public static final ITriggerInternalSided LIGHT_DARK = StatementManager.registerStatement(new LightTrigger(false));
    public static final ITriggerInternal[] TIMERS = {
        StatementManager.registerStatement(new TimerTrigger("short", 5)),
        StatementManager.registerStatement(new TimerTrigger("medium", 10)),
        StatementManager.registerStatement(new TimerTrigger("long", 15)) };
    public static final IActionInternalSided PULSAR_CONSTANT = StatementManager.registerStatement(new PulsarAction(false));
    public static final IActionInternalSided PULSAR_SINGLE = StatementManager.registerStatement(new PulsarAction(true));

    public static void init() {
        StatementManager.registerProvider(new IStatementProvider() {
            @Override
            public void addInternalTriggers(Collection<ITriggerInternal> triggers, IStatementContainer container) {
                if (container instanceof IGate gate) {
                    for (Direction side : Direction.values()) {
                        PipePluggable plug = gate.getPipeHolder().getPluggable(side);
                        if (plug != null && plug.definition == BCSiliconPlugs.TIMER) {
                            triggers.addAll(java.util.List.of(TIMERS));
                            return;
                        }
                    }
                }
            }

            @Override
            public void addInternalSidedTriggers(Collection<ITriggerInternalSided> triggers, IStatementContainer container, Direction side) {
                if (container instanceof IGate gate) {
                    PipePluggable plug = gate.getPipeHolder().getPluggable(side);
                    if (plug != null && plug.definition == BCSiliconPlugs.LIGHT_SENSOR) {
                        triggers.add(LIGHT_BRIGHT);
                        triggers.add(LIGHT_DARK);
                    }
                }
            }

            @Override
            public void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container, Direction side) {
                if (container instanceof IGate gate && gate.getPipeHolder().getPluggable(side) instanceof PluggablePulsar) {
                    actions.add(PULSAR_CONSTANT);
                    actions.add(PULSAR_SINGLE);
                }
            }
        });
    }

    static class LightTrigger extends BCStatement implements ITriggerInternalSided {
        private final boolean bright;

        LightTrigger(boolean bright) {
            super("light." + (bright ? "bright" : "dark"), "gate.buildcraft.trigger.light." + (bright ? "bright" : "dark"),
                bright ? "trigger_light_bright" : "trigger_light_dark");
            this.bright = bright;
        }

        @Override
        public boolean isTriggerActive(Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            BlockPos pos = source.getPos().relative(side);
            int light = source.getLevel().getMaxLocalRawBrightness(pos);
            return (light < 8) ^ bright;
        }
    }

    static class TimerTrigger extends BCStatement implements ITriggerInternal {
        private final int seconds;

        TimerTrigger(String name, int seconds) {
            super("timer." + name, "gate.buildcraft.trigger.timer", "trigger_timer_" + name);
            this.seconds = seconds;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.trigger.timer", seconds);
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            return source.getLevel().getGameTime() % (20L * seconds) == 0;
        }
    }

    static class PulsarAction extends BCStatement implements IActionInternalSided {
        private final boolean single;

        PulsarAction(boolean single) {
            super("pulsar." + (single ? "single" : "constant"), "gate.buildcraft.action.pulsar." + (single ? "single" : "constant"),
                single ? "action_pulsar_single" : "action_pulsar_on");
            this.single = single;
        }

        @Override
        public void actionActivate(Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate && gate.getPipeHolder().getPluggable(side) instanceof PluggablePulsar pulsar) {
                if (single) {
                    pulsar.addSinglePulse();
                } else {
                    pulsar.enablePulsar();
                }
            }
        }
    }
}
