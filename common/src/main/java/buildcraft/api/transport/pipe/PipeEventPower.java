/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import buildcraft.api.mj.MjAPI;

public abstract class PipeEventPower extends PipeEvent {
    public final IFlowPower flow;

    protected PipeEventPower(IPipeHolder holder, IFlowPower flow) {
        super(holder);
        this.flow = flow;
    }

    /** Fired when a power pipe works out how much it can carry. */
    public static class Configure extends PipeEventPower {
        private long maxPower = 10 * MjAPI.MJ;
        private long powerResistance = -1;
        private long powerLoss = -1;
        private boolean receiver = false;
        private boolean disabled = false;

        public Configure(IPipeHolder holder, IFlowPower flow) {
            super(holder, flow);
        }

        public long getMaxPower() {
            return maxPower;
        }

        public void setMaxPower(long maxPower) {
            this.maxPower = maxPower;
        }

        public long getPowerLoss() {
            return powerLoss;
        }

        public void setPowerLoss(long powerLoss) {
            this.powerLoss = powerLoss;
        }

        public long getPowerResistance() {
            return powerResistance;
        }

        public void setPowerResistance(long powerResistance) {
            this.powerResistance = powerResistance;
        }

        public boolean isReceiver() {
            return receiver;
        }

        public void setReceiver(boolean receiver) {
            this.receiver = receiver;
        }

        public void disableTransfer() {
            disabled = true;
        }

        public boolean isTransferDisabled() {
            return disabled;
        }
    }
}
