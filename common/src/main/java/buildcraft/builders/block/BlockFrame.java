/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The frame that a quarry builds around the area it mines. */
public class BlockFrame extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTED = new EnumMap<>(Direction.class);
    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (Direction side : Direction.values()) {
            CONNECTED.put(side, BooleanProperty.create("connected_" + side.getSerializedName()));
        }
        ARMS.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
        ARMS.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        ARMS.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        ARMS.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        ARMS.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
        ARMS.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        for (int i = 0; i < 64; i++) {
            VoxelShape shape = CENTER;
            for (Direction side : Direction.values()) {
                if ((i & (1 << side.get3DDataValue())) != 0) {
                    shape = Shapes.or(shape, ARMS.get(side));
                }
            }
            SHAPES[i] = shape;
        }
    }

    public BlockFrame(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTED.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
    }

    private static boolean connectsTo(BlockState other) {
        return other.getBlock() instanceof BlockFrame || other.getBlock() instanceof BlockQuarry;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTED.get(side),
                connectsTo(context.getLevel().getBlockState(context.getClickedPos().relative(side))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
        BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return state.setValue(CONNECTED.get(direction), connectsTo(neighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        for (Direction side : Direction.values()) {
            if (state.getValue(CONNECTED.get(side))) {
                index |= 1 << side.get3DDataValue();
            }
        }
        return SHAPES[index];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
