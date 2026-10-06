/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import buildcraft.core.tile.TileMarkerVolume;

/** A volume marker. Powering it with redstone shows where it can connect. */
public class BlockMarkerVolume extends BlockMarker<TileMarkerVolume> {
    public BlockMarkerVolume(Properties properties, Supplier<BlockEntityType<TileMarkerVolume>> tileType) {
        super(properties, tileType);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
        boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.isClientSide()) return;
        TileMarkerVolume tile = getTile(level, pos);
        if (tile != null) {
            tile.setShowSignals(level.hasNeighborSignal(pos));
        }
    }
}
