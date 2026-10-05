/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import buildcraft.builders.tile.TileArchitectTable;

/** Scans the area marked out behind it. Its front lights up when it has an area. */
public class BlockArchitectTable extends BlockFacing<TileArchitectTable> {
    public static final BooleanProperty VALID = BooleanProperty.create("valid");

    public BlockArchitectTable(Properties properties, Supplier<BlockEntityType<TileArchitectTable>> tileType) {
        super(properties, tileType);
        registerDefaultState(defaultBlockState().setValue(VALID, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VALID);
    }
}
