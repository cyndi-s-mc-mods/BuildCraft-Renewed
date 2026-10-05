/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

/** Something that accepts MJ power. All power values are in micro MJ (see {@link MjAPI#MJ}). */
public interface IMjReceiver extends IMjConnector {
    /** @return How much power this receiver would like to receive right now. */
    long getPowerRequested();

    /** Gives power to this receiver.
     *
     * @return The excess power that could not be accepted. */
    long receivePower(long microJoules, boolean simulate);

    default boolean canReceive() {
        return true;
    }
}
