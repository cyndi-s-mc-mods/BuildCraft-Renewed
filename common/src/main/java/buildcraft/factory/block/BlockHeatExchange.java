/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.block.IWrenchable;

/** A heat exchanger is a line of 3 to 5 of these blocks, all facing the same way. The start heats one fluid using the
 * heat of the fluid put into the end. */
public class BlockHeatExchange extends BlockBCTile<TileHeatExchange> implements IWrenchable {
    public enum Part implements StringRepresentable {
        START,
        MIDDLE,
        END;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Connected towards the start (clockwise from the facing). */
    public static final BooleanProperty CONNECTED_LEFT = BooleanProperty.create("connected_left");
    /** Connected towards the end (anticlockwise from the facing). */
    public static final BooleanProperty CONNECTED_RIGHT = BooleanProperty.create("connected_right");

    public BlockHeatExchange(Properties properties, Supplier<BlockEntityType<TileHeatExchange>> tileType) {
        super(properties.noOcclusion(), tileType);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST).setValue(PART, Part.MIDDLE)
            .setValue(CONNECTED_LEFT, false).setValue(CONNECTED_RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, CONNECTED_LEFT, CONNECTED_RIGHT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
        BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return updateConnections(state, level, pos);
    }

    private static BlockState updateConnections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(CONNECTED_LEFT, connects(level, pos, facing, facing.getClockWise()))
            .setValue(CONNECTED_RIGHT, connects(level, pos, facing, facing.getCounterClockWise()));
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction facing, Direction dir) {
        BlockState other = level.getBlockState(pos.relative(dir));
        return other.getBlock() instanceof BlockHeatExchange && other.getValue(FACING) == facing;
    }

    @Override
    public InteractionResult onWrench(BlockState state, Level level, BlockPos pos, Player player, Direction side) {
        TileHeatExchange tile = getTile(level, pos);
        if (tile == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            tile.rotate();
        }
        return InteractionResult.SUCCESS;
    }
}
