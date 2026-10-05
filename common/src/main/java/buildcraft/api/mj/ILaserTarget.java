/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

/** A block entity that lasers can power, such as the assembly table. */
public interface ILaserTarget {
    long getRequiredLaserPower();

    /** @return The excess power that was not used. */
    long receiveLaserPower(long microJoules);

    boolean isInvalidTarget();
}
