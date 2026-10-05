/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.IdentityHashMap;
import java.util.Map;

import buildcraft.api.mj.MjAPI;

/** Transfer rates for each pipe definition, filled in by BuildCraft Transport. */
public final class PipeApi {
    public static FluidTransferInfo fluidInfoDefault = new FluidTransferInfo(20, 10);
    public static PowerTransferInfo powerInfoDefault = PowerTransferInfo.createFromResistance(8 * MjAPI.MJ, MjAPI.MJ / 32, false);

    public static final Map<PipeDefinition, FluidTransferInfo> fluidTransferData = new IdentityHashMap<>();
    public static final Map<PipeDefinition, PowerTransferInfo> powerTransferData = new IdentityHashMap<>();

    private PipeApi() {}

    public static FluidTransferInfo getFluidTransferInfo(PipeDefinition def) {
        return fluidTransferData.getOrDefault(def, fluidInfoDefault);
    }

    public static PowerTransferInfo getPowerTransferInfo(PipeDefinition def) {
        return powerTransferData.getOrDefault(def, powerInfoDefault);
    }

    /** @param transferPerTick Millibuckets moved each tick.
     * @param transferDelayMultiplier Ticks fluid takes to move through each section. */
    public record FluidTransferInfo(int transferPerTick, double transferDelayMultiplier) {
        public FluidTransferInfo {
            transferPerTick = Math.max(1, transferPerTick);
            transferDelayMultiplier = Math.max(1, transferDelayMultiplier);
        }
    }

    public record PowerTransferInfo(long transferPerTick, long lossPerTick, long resistancePerTick, boolean isReceiver) {
        public PowerTransferInfo {
            transferPerTick = Math.max(10, transferPerTick);
        }

        public static PowerTransferInfo createFromResistance(long transferPerTick, long resistancePerTick, boolean isReceiver) {
            return new PowerTransferInfo(transferPerTick, resistancePerTick * transferPerTick / MjAPI.MJ, resistancePerTick, isReceiver);
        }
    }
}
