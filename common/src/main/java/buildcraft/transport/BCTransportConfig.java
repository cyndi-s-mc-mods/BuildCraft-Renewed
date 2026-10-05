/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.api.mj.MjAPI;

public final class BCTransportConfig {
    /** Power (in micro MJ) a wooden pipe needs to extract a millibucket of fluid. */
    public static long mjPerMillibucket = 1_000;
    /** Power (in micro MJ) a wooden pipe needs to extract an item. */
    public static long mjPerItem = MjAPI.MJ;
    public static int baseFlowRate = 10;
    public static int basePowerRate = 4;

    private BCTransportConfig() {}
}
