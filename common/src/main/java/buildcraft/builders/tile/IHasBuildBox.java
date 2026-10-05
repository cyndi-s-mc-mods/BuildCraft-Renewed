/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** A machine that works on an area, which is drawn as a box of lasers. */
public interface IHasBuildBox {
    @Nullable
    BoundingBox getBox();
}
