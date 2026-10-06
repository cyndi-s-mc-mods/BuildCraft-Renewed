/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics;

import net.minecraft.world.inventory.MenuType;

import buildcraft.lib.registry.RegistryEntry;
import buildcraft.robotics.container.ContainerZonePlanner;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCRoboticsMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerZonePlanner>> ZONE_PLANNER;

    private BCRoboticsMenus() {}

    static void init() {
        ZONE_PLANNER = menu("zone_planner", ContainerZonePlanner::new);
    }
}
