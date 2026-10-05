/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import buildcraft.factory.tile.TilePump;
import buildcraft.lib.block.BlockBCTile;

public class BlockPump extends BlockBCTile<TilePump> {
    public BlockPump(Properties properties, Supplier<BlockEntityType<TilePump>> tileType) {
        super(properties, tileType);
    }
}
