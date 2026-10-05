/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

/** Something that MJ power can be pulled out of. */
public interface IMjPassiveProvider extends IMjConnector {
    /** @return The power extracted, between min and max, or 0 if less than min was available. */
    long extractPower(long min, long max, boolean simulate);
}
