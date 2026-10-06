/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.marker;

/** Implemented by block entities that lock a volume box while they use it, so that it can't be moved or removed. */
public interface IVolumeBoxUser {
    boolean isUsing(VolumeBoxEntity box);
}
