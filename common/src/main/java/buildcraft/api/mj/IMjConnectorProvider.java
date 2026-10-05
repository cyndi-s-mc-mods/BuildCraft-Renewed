/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/** Implemented by block entities that take part in the MJ power system. This replaces the old Forge capabilities. */
public interface IMjConnectorProvider {
    /** @param side The side of this block that is being connected to.
     * @return The connector on that side, or null if there is nothing to connect to there. */
    @Nullable
    IMjConnector getMjConnector(Direction side);
}
