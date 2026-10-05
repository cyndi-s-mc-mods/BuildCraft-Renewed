/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.power;

/** Implemented by block entities (such as laser tables) that lasers can power. */
public interface ILaserTarget {
    /** @return The power (in micro MJ) that this target wants right now. 0 if it doesn't need any. */
    long getRequiredLaserPower();

    /** @return The power that wasn't used. */
    long receiveLaserPower(long microJoules);
}
