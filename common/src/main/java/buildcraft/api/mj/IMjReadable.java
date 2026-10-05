/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

/** Something that stores MJ and can report how much it has. */
public interface IMjReadable extends IMjConnector {
    long getStored();

    long getCapacity();
}
