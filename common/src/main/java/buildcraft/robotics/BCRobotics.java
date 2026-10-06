/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics;

import buildcraft.robotics.zone.ZonePackets;

/** The robotics module. BuildCraft 7.99 had no robots, only the zone planner. */
public final class BCRobotics {
    private BCRobotics() {}

    public static void init() {
        BCRoboticsBlocks.init();
        BCRoboticsMenus.init();
        ZonePackets.init();
    }
}
