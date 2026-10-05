/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

/** Anything that can be connected to the MJ power system. */
public interface IMjConnector {
    /** @return True if this connector can connect to the other one. Both sides must agree for power to flow. */
    boolean canConnect(IMjConnector other);
}
