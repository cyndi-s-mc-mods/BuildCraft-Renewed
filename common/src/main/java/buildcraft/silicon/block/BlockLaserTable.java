/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.silicon.tile.TileLaserTableBase;

/** The base of the assembly, advanced crafting and integration tables. */
public class BlockLaserTable<T extends TileLaserTableBase> extends BlockBCTile<T> {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    public BlockLaserTable(Properties properties, Supplier<BlockEntityType<T>> tileType) {
        super(properties.noOcclusion(), tileType);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
