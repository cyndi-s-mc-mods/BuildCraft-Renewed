/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.Arrays;
import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

import buildcraft.lib.fluid.BCFluidStack;

public abstract class PipeEventFluid extends PipeEvent {
    public final IFlowFluid flow;

    protected PipeEventFluid(IPipeHolder holder, IFlowFluid flow) {
        super(holder);
        this.flow = flow;
    }

    protected PipeEventFluid(boolean canBeCancelled, IPipeHolder holder, IFlowFluid flow) {
        super(canBeCancelled, holder);
        this.flow = flow;
    }

    public static class TryInsert extends PipeEventFluid {
        public final Direction from;
        public final BCFluidStack fluid;

        public TryInsert(IPipeHolder holder, IFlowFluid flow, Direction from, BCFluidStack fluid) {
            super(true, holder, flow);
            this.from = from;
            this.fluid = fluid;
        }
    }

    /** Fired before fluid moves from the sides into the centre. Lower {@link #actuallyOffered} to slow it down. */
    public static class PreMoveToCentre extends PipeEventFluid {
        public final BCFluidStack fluid;
        public final int totalAcceptable;
        public final int[] totalOffered;
        private final int[] totalOfferedCheck;
        public final int[] actuallyOffered;

        public PreMoveToCentre(IPipeHolder holder, IFlowFluid flow, BCFluidStack fluid, int totalAcceptable,
            int[] totalOffered, int[] actuallyOffered) {
            super(holder, flow);
            this.fluid = fluid;
            this.totalAcceptable = totalAcceptable;
            this.totalOffered = totalOffered;
            this.totalOfferedCheck = Arrays.copyOf(totalOffered, totalOffered.length);
            this.actuallyOffered = actuallyOffered;
        }

        @Override
        public @Nullable String checkStateForErrors() {
            for (int i = 0; i < totalOffered.length; i++) {
                if (totalOffered[i] != totalOfferedCheck[i]) {
                    return "Changed totalOffered";
                }
                if (actuallyOffered[i] > totalOffered[i]) {
                    return "actuallyOffered[" + i + "] shouldn't be greater than totalOffered[" + i + "]";
                }
            }
            return super.checkStateForErrors();
        }
    }

    /** Fired as fluid moves into the centre. Lower {@link #fluidEnteringCentre} to destroy some of it (void pipes). */
    public static class OnMoveToCentre extends PipeEventFluid {
        public final BCFluidStack fluid;
        public final int[] fluidLeavingSide;
        public final int[] fluidEnteringCentre;

        public OnMoveToCentre(IPipeHolder holder, IFlowFluid flow, BCFluidStack fluid, int[] fluidLeavingSide,
            int[] fluidEnteringCentre) {
            super(holder, flow);
            this.fluid = fluid;
            this.fluidLeavingSide = fluidLeavingSide;
            this.fluidEnteringCentre = fluidEnteringCentre;
        }
    }

    /** Fired to decide which sides fluid may leave by. */
    public static class SideCheck extends PipeEventFluid {
        public final BCFluidStack fluid;
        public final SideOrder order = new SideOrder();

        public SideCheck(IPipeHolder holder, IFlowFluid flow, BCFluidStack fluid) {
            super(holder, flow);
            this.fluid = fluid;
        }

        public boolean isAllowed(Direction side) {
            return order.isAllowed(side);
        }

        public void disallow(Direction... sides) {
            order.disallow(sides);
        }

        public void disallowAllExcept(Direction... sides) {
            order.disallowAllExcept(sides);
        }

        public void disallowAllExcept(java.util.Collection<Direction> sides) {
            order.disallowAllExcept(sides);
        }

        public void disallowAll() {
            order.disallowAll();
        }

        public void increasePriority(Direction side, int by) {
            order.increasePriority(side, by);
        }

        public EnumSet<Direction> getOrder() {
            return order.getHighestPriority();
        }
    }
}
