/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import java.util.Collection;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.IStatementProvider;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IHasWork;
import buildcraft.lib.engine.TileEngineBase;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.statement.BCStatement;
import buildcraft.lib.statement.StatementParamGateSideOnly;
import buildcraft.lib.statement.StatementParamItemStack;

/** The general triggers and actions: redstone, machines, engines, inventories and tanks. */
public final class BCCoreStatements {
    private BCCoreStatements() {}

    // Triggers

    public static final ITriggerInternal TRUE = StatementManager.registerStatement(new InternalTrigger("true", "trigger_true",
        (c, p) -> true));
    public static final ITriggerInternal REDSTONE_ACTIVE = StatementManager.registerStatement(new RedstoneTrigger(true));
    public static final ITriggerInternal REDSTONE_INACTIVE = StatementManager.registerStatement(new RedstoneTrigger(false));

    public static final ITriggerExternal MACHINE_ACTIVE = StatementManager.registerStatement(
        new ExternalTrigger("machine.scheduled", "trigger_machine_active", (t, s, c, p) -> t instanceof IHasWork w && w.hasWork()));
    public static final ITriggerExternal MACHINE_INACTIVE = StatementManager.registerStatement(
        new ExternalTrigger("machine.done", "trigger_machine_inactive", (t, s, c, p) -> t instanceof IHasWork w && !w.hasWork()));

    public static final ITriggerExternal[] ENGINE_STAGES = new ITriggerExternal[5];
    public static final ITriggerExternal ENERGY_HIGH = StatementManager.registerStatement(
        new ExternalTrigger("energy.high", "trigger_energy_storage_high", (t, s, c, p) -> energyLevel(t, s) > 0.95));
    public static final ITriggerExternal ENERGY_LOW = StatementManager.registerStatement(
        new ExternalTrigger("energy.low", "trigger_energy_storage_low", (t, s, c, p) -> {
            double level = energyLevel(t, s);
            return level >= 0 && level < 0.05;
        }));

    /** @return How full (0 to 1) the machine's power store is, or -1 if it doesn't say. */
    static double energyLevel(BlockEntity tile, Direction side) {
        if (tile instanceof buildcraft.api.mj.IMjConnectorProvider provider
            && provider.getMjConnector(side) instanceof buildcraft.api.mj.IMjReadable readable && readable.getCapacity() > 0) {
            return readable.getStored() / (double) readable.getCapacity();
        }
        return -1;
    }

    static {
        String[] icons = { "trigger_engineheat_blue", "trigger_engineheat_green", "trigger_engineheat_yellow", "trigger_engineheat_red",
            "trigger_engineheat_overheat" };
        for (int i = 0; i < 5; i++) {
            EnumPowerStage stage = EnumPowerStage.values()[i];
            ENGINE_STAGES[i] = StatementManager.registerStatement(new ExternalTrigger("engine." + stage.getSerializedName(), icons[i],
                (t, s, c, p) -> t instanceof TileEngineBase engine && engine.getPowerStage() == stage));
        }
    }

    public static final ITriggerExternal INVENTORY_EMPTY = StatementManager.registerStatement(new InventoryTrigger("empty", 0));
    public static final ITriggerExternal INVENTORY_CONTAINS = StatementManager.registerStatement(new InventoryTrigger("contains", 1));
    public static final ITriggerExternal INVENTORY_SPACE = StatementManager.registerStatement(new InventoryTrigger("space", 1));
    public static final ITriggerExternal INVENTORY_FULL = StatementManager.registerStatement(new InventoryTrigger("full", 0));

    public static final ITriggerExternal FLUID_EMPTY = StatementManager.registerStatement(new FluidTrigger("empty"));
    public static final ITriggerExternal FLUID_CONTAINS = StatementManager.registerStatement(new FluidTrigger("contains"));
    public static final ITriggerExternal FLUID_SPACE = StatementManager.registerStatement(new FluidTrigger("space"));
    public static final ITriggerExternal FLUID_FULL = StatementManager.registerStatement(new FluidTrigger("full"));
    public static final ITriggerExternal[] FLUID_BELOW = {
        StatementManager.registerStatement(new FluidLevelTrigger(25)),
        StatementManager.registerStatement(new FluidLevelTrigger(50)),
        StatementManager.registerStatement(new FluidLevelTrigger(75)) };
    public static final ITriggerExternal[] INVENTORY_BELOW = {
        StatementManager.registerStatement(new InventoryLevelTrigger(25)),
        StatementManager.registerStatement(new InventoryLevelTrigger(50)),
        StatementManager.registerStatement(new InventoryLevelTrigger(75)) };

    // Actions

    public static final IActionInternal REDSTONE_OUTPUT = StatementManager.registerStatement(new RedstoneOutputAction());
    public static final IActionExternal[] MACHINE_CONTROL = {
        StatementManager.registerStatement(new MachineControlAction(IControllable.Mode.ON)),
        StatementManager.registerStatement(new MachineControlAction(IControllable.Mode.OFF)),
        StatementManager.registerStatement(new MachineControlAction(IControllable.Mode.LOOP)) };

    public static void init() {
        StatementManager.registerParameter(StatementParamItemStack.TAG, StatementParamItemStack::load);
        StatementManager.registerParameter(StatementParamGateSideOnly.TAG, StatementParamGateSideOnly::load);
        StatementManager.registerProvider(new IStatementProvider() {
            @Override
            public void addInternalTriggers(Collection<ITriggerInternal> triggers, IStatementContainer container) {
                triggers.add(TRUE);
                if (container instanceof IGate) {
                    triggers.add(REDSTONE_ACTIVE);
                    triggers.add(REDSTONE_INACTIVE);
                }
            }

            @Override
            public void addExternalTriggers(Collection<ITriggerExternal> triggers, Direction side, BlockEntity tile) {
                if (energyLevel(tile, side) >= 0) {
                    triggers.add(ENERGY_HIGH);
                    triggers.add(ENERGY_LOW);
                }
                if (tile instanceof IHasWork) {
                    triggers.add(MACHINE_ACTIVE);
                    triggers.add(MACHINE_INACTIVE);
                }
                if (tile instanceof TileEngineBase) {
                    triggers.addAll(java.util.List.of(ENGINE_STAGES));
                }
                if (tile.getLevel() != null && Platform.INSTANCE.getItemTransactor(tile.getLevel(), tile.getBlockPos(), side) != null) {
                    triggers.add(INVENTORY_EMPTY);
                    triggers.add(INVENTORY_CONTAINS);
                    triggers.add(INVENTORY_SPACE);
                    triggers.add(INVENTORY_FULL);
                    if (tile instanceof Container) {
                        triggers.addAll(java.util.List.of(INVENTORY_BELOW));
                    }
                }
                if (tile.getLevel() != null && Platform.INSTANCE.getFluidHandler(tile.getLevel(), tile.getBlockPos(), side) != null) {
                    triggers.add(FLUID_EMPTY);
                    triggers.add(FLUID_CONTAINS);
                    triggers.add(FLUID_SPACE);
                    triggers.add(FLUID_FULL);
                    triggers.addAll(java.util.List.of(FLUID_BELOW));
                }
            }

            @Override
            public void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {
                if (container instanceof IGate) {
                    actions.add(REDSTONE_OUTPUT);
                }
            }

            @Override
            public void addExternalActions(Collection<buildcraft.api.statements.IActionExternal> actions, Direction side, BlockEntity tile) {
                if (tile instanceof IControllable controllable) {
                    for (IActionExternal action : MACHINE_CONTROL) {
                        if (controllable.acceptsControlMode(((MachineControlAction) action).mode)) {
                            actions.add(action);
                        }
                    }
                }
            }
        });
    }

    // Implementations

    @FunctionalInterface
    interface InternalCheck {
        boolean test(IStatementContainer container, IStatementParameter[] params);
    }

    @FunctionalInterface
    interface ExternalCheck {
        boolean test(BlockEntity target, Direction side, IStatementContainer container, IStatementParameter[] params);
    }

    static class InternalTrigger extends BCStatement implements ITriggerInternal {
        private final InternalCheck check;

        InternalTrigger(String name, String icon, InternalCheck check) {
            super(name, "gate.buildcraft.trigger." + name, icon);
            this.check = check;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            return check.test(source, parameters);
        }
    }

    static class ExternalTrigger extends BCStatement implements ITriggerExternal {
        private final ExternalCheck check;

        ExternalTrigger(String name, String icon, ExternalCheck check) {
            super(name, "gate.buildcraft.trigger." + name, icon);
            this.check = check;
        }

        @Override
        public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            return check.test(target, side, source, parameters);
        }
    }

    static class RedstoneTrigger extends BCStatement implements ITriggerInternal {
        private final boolean active;

        RedstoneTrigger(boolean active) {
            super("redstone.input." + (active ? "active" : "inactive"), "gate.buildcraft.trigger.redstone.input." + (active ? "active" : "inactive"),
                active ? "trigger_redstoneinput_active" : "trigger_redstoneinput_inactive");
            this.active = active;
        }

        @Override
        public int maxParameters() {
            return 1;
        }

        @Override
        public IStatementParameter createParameter(int index) {
            return StatementParamGateSideOnly.ANY;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            if (!(source instanceof IGate gate)) return false;
            int level;
            if (StatementParamGateSideOnly.isSideOnly(parameters)) {
                Direction side = gate.getSide();
                level = gate.getLevel().getSignal(gate.getPos().relative(side), side);
            } else {
                level = gate.getRedstoneInput();
            }
            return (level > 0) == active;
        }
    }

    static class InventoryTrigger extends BCStatement implements ITriggerExternal {
        private final String state;
        private final int params;

        InventoryTrigger(String state, int params) {
            super("inventory." + state, "gate.buildcraft.trigger.inventory." + state, "trigger_inventory_" + state);
            this.state = state;
            this.params = params;
        }

        @Override
        public int maxParameters() {
            return params;
        }

        @Override
        public @Nullable IStatementParameter createParameter(int index) {
            return params > 0 ? StatementParamItemStack.EMPTY : null;
        }

        @Override
        public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (target.getLevel() == null) return false;
            IItemTransactor inv = Platform.INSTANCE.getItemTransactor(target.getLevel(), target.getBlockPos(), side);
            if (inv == null) return false;
            ItemStack filter = parameters.length > 0 && parameters[0] instanceof StatementParamItemStack p ? p.stack : ItemStack.EMPTY;
            return switch (state) {
                case "empty" -> inv.extract(s -> true, 1, 1, true).isEmpty();
                case "contains" -> !inv.extract(s -> filter.isEmpty() || ItemStack.isSameItemSameComponents(s, filter), 1, 1, true).isEmpty();
                case "space" -> {
                    ItemStack test = filter.isEmpty() ? new ItemStack(Items.COBBLESTONE) : filter.copyWithCount(1);
                    yield inv.insert(test, true).isEmpty();
                }
                default -> {
                    // Full: nothing more fits, even of what's already inside
                    ItemStack inside = inv.extract(s -> true, 1, 1, true);
                    ItemStack test = inside.isEmpty() ? new ItemStack(Items.COBBLESTONE) : inside;
                    yield !inv.insert(test, true).isEmpty();
                }
            };
        }
    }

    static class InventoryLevelTrigger extends BCStatement implements ITriggerExternal {
        private final int percent;

        InventoryLevelTrigger(int percent) {
            super("inventorylevel.below" + percent, "gate.buildcraft.trigger.inventorylevel.below", "trigger_inventory_below" + percent);
            this.percent = percent;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.trigger.inventorylevel.below", percent);
        }

        @Override
        public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (!(target instanceof Container container)) return false;
            long count = 0, max = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                count += stack.getCount();
                max += stack.isEmpty() ? container.getMaxStackSize() : Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
            }
            return max > 0 && count * 100 < max * percent;
        }
    }

    static class FluidTrigger extends BCStatement implements ITriggerExternal {
        private final String state;

        FluidTrigger(String state) {
            super("fluid." + state, "gate.buildcraft.trigger.fluid." + state, "trigger_liquidcontainer_" + state);
            this.state = state;
        }

        @Override
        public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (target.getLevel() == null) return false;
            IFluidHandlerBC handler = Platform.INSTANCE.getFluidHandler(target.getLevel(), target.getBlockPos(), side);
            if (handler == null) return false;
            boolean anyFluid = false, anySpace = false, allEmpty = true, allFull = true;
            for (int t = 0; t < handler.getTanks(); t++) {
                BCFluidStack fluid = handler.getFluidInTank(t);
                int cap = handler.getTankCapacity(t);
                if (!fluid.isEmpty()) {
                    anyFluid = true;
                    allEmpty = false;
                }
                if (fluid.getAmount() < cap) {
                    anySpace = true;
                    allFull = false;
                }
            }
            return switch (state) {
                case "empty" -> allEmpty;
                case "contains" -> anyFluid;
                case "space" -> anySpace;
                default -> allFull;
            };
        }
    }

    static class FluidLevelTrigger extends BCStatement implements ITriggerExternal {
        private final int percent;

        FluidLevelTrigger(int percent) {
            super("fluidlevel.below" + percent, "gate.buildcraft.trigger.fluidlevel.below", "trigger_liquidcontainer_below" + percent);
            this.percent = percent;
        }

        @Override
        public Component getDescription() {
            return Component.translatable("gate.buildcraft.trigger.fluidlevel.below", percent);
        }

        @Override
        public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (target.getLevel() == null) return false;
            IFluidHandlerBC handler = Platform.INSTANCE.getFluidHandler(target.getLevel(), target.getBlockPos(), side);
            if (handler == null) return false;
            long amount = 0, cap = 0;
            for (int t = 0; t < handler.getTanks(); t++) {
                amount += handler.getFluidInTank(t).getAmount();
                cap += handler.getTankCapacity(t);
            }
            return cap > 0 && amount * 100 < cap * percent;
        }
    }

    static class RedstoneOutputAction extends BCStatement implements IActionInternal {
        RedstoneOutputAction() {
            super("redstone.output", "gate.buildcraft.action.redstone.signal", "action_redstoneoutput");
        }

        @Override
        public int maxParameters() {
            return 1;
        }

        @Override
        public IStatementParameter createParameter(int index) {
            return StatementParamGateSideOnly.ANY;
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (source instanceof IGate gate) {
                gate.setRedstoneOutput(15, StatementParamGateSideOnly.isSideOnly(parameters));
            }
        }
    }

    static class MachineControlAction extends BCStatement implements IActionExternal {
        final IControllable.Mode mode;

        MachineControlAction(IControllable.Mode mode) {
            super("machine." + mode.name().toLowerCase(Locale.ROOT), "gate.buildcraft.action.machine." + mode.name().toLowerCase(Locale.ROOT),
                "action_machinecontrol_" + mode.name().toLowerCase(Locale.ROOT));
            this.mode = mode;
        }

        @Override
        public void actionActivate(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (target instanceof IControllable controllable && controllable.acceptsControlMode(mode)) {
                controllable.setControlMode(mode);
            }
        }

        @Override
        public void actionDeactivated(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
            if (target instanceof IControllable controllable && controllable.getControlMode() == mode) {
                controllable.setControlMode(IControllable.Mode.UNKNOWN);
            }
        }
    }
}
