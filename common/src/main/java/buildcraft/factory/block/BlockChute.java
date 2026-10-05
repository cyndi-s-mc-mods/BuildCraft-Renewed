/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.factory.tile.TileChute;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.platform.Platform;

/** A chute collects items in front of it and pushes them into the inventories around it. */
public class BlockChute extends BlockBCTile<TileChute> {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final Map<Direction, BooleanProperty> CONNECTED = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            CONNECTED.put(side, BooleanProperty.create("connected_" + side.getSerializedName()));
        }
    }

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // The open top of the chute, then the funnel narrowing down to the spout
        SHAPES.put(Direction.UP, Shapes.or(Block.box(0, 9, 0, 16, 16, 16), Block.box(3, 3, 3, 13, 9, 13), Block.box(5, 0, 5, 11, 3, 11)));
        SHAPES.put(Direction.DOWN, Shapes.or(Block.box(0, 0, 0, 16, 7, 16), Block.box(3, 7, 3, 13, 13, 13), Block.box(5, 13, 5, 11, 16, 11)));
        SHAPES.put(Direction.NORTH, Shapes.or(Block.box(0, 0, 0, 16, 16, 7), Block.box(3, 3, 7, 13, 13, 13), Block.box(5, 5, 13, 11, 11, 16)));
        SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(0, 0, 9, 16, 16, 16), Block.box(3, 3, 3, 13, 13, 9), Block.box(5, 5, 0, 11, 11, 3)));
        SHAPES.put(Direction.WEST, Shapes.or(Block.box(0, 0, 0, 7, 16, 16), Block.box(7, 3, 3, 13, 13, 13), Block.box(13, 5, 5, 16, 11, 11)));
        SHAPES.put(Direction.EAST, Shapes.or(Block.box(9, 0, 0, 16, 16, 16), Block.box(3, 3, 3, 9, 13, 13), Block.box(0, 5, 5, 3, 11, 11)));
    }

    public BlockChute(Properties properties, Supplier<BlockEntityType<TileChute>> tileType) {
        super(properties.noOcclusion(), tileType);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.UP);
        for (BooleanProperty property : CONNECTED.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        for (Direction side : Direction.values()) {
            state = updateConnection(state, context.getLevel(), context.getClickedPos(), side);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
        BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return updateConnection(state, level, pos, direction);
    }

    private static BlockState updateConnection(BlockState state, LevelReader reader, BlockPos pos, Direction side) {
        boolean connected = false;
        if (side != state.getValue(FACING) && reader instanceof Level level && level.getBlockEntity(pos.relative(side)) != null) {
            connected = Platform.INSTANCE.getItemTransactor(level, pos.relative(side), side.getOpposite()) != null;
        }
        return state.setValue(CONNECTED.get(side), connected);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        for (Direction side : Direction.values()) {
            rotated = rotated.setValue(CONNECTED.get(rotation.rotate(side)), state.getValue(CONNECTED.get(side)));
        }
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
