/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.tiles;

import net.minecraft.core.BlockPos;

/** Implemented by block entities (such as volume markers) that mark out an area for machines like the quarry. */
public interface IAreaProvider {
    BlockPos min();

    BlockPos max();

    /** Removes the markers that make up the area, dropping them as items. */
    void removeFromWorld();

    /** @return True if a machine at the given position may use this area: it must be next to a corner of the area and
     *         not inside it. */
    boolean isValidFromLocation(BlockPos pos);
}
