/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.silicon.tile.TileLaser;

/** A laser, which faces away from the block it was placed on. */
public class BlockLaser extends BlockBCTile<TileLaser> {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(5, 4, 5, 11, 13, 11)));
        SHAPES.put(Direction.DOWN, Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(5, 3, 5, 11, 12, 11)));
        SHAPES.put(Direction.NORTH, Shapes.or(Block.box(0, 0, 12, 16, 16, 16), Block.box(5, 5, 3, 11, 11, 12)));
        SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(0, 0, 0, 16, 16, 4), Block.box(5, 5, 4, 11, 11, 13)));
        SHAPES.put(Direction.WEST, Shapes.or(Block.box(12, 0, 0, 16, 16, 16), Block.box(3, 5, 5, 12, 11, 11)));
        SHAPES.put(Direction.EAST, Shapes.or(Block.box(0, 0, 0, 4, 16, 16), Block.box(4, 5, 5, 13, 11, 11)));
    }

    public BlockLaser(Properties properties, Supplier<BlockEntityType<TileLaser>> tileType) {
        super(properties.noOcclusion(), tileType);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
