/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.core;

import java.util.List;

import net.minecraft.core.BlockPos;

/** Implemented by block entities (such as path markers) that mark out a path, for machines like the builder. */
public interface IPathProvider {
    /** @return The positions along the path, in order. A path that loops ends with its first position again. */
    List<BlockPos> getPath();

    /** Removes the markers that make up the path, dropping them as items. */
    void removeFromWorld();
}
