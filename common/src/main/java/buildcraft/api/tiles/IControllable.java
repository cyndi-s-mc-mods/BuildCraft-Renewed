/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.tiles;

/** Implemented by machines that gates can switch on and off. */
public interface IControllable {
    enum Mode {
        /** Normal operation. */
        UNKNOWN,
        ON,
        OFF,
        /** Repeat the job when it finishes. */
        LOOP
    }

    Mode getControlMode();

    void setControlMode(Mode mode);

    default boolean acceptsControlMode(Mode mode) {
        return true;
    }
}
