/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import buildcraft.factory.tile.TileFloodGate;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.block.IWrenchable;

/** Flood gates fill the area around them from the sides that are open. Every side but the top can be opened or closed
 * with a wrench. */
public class BlockFloodGate extends BlockBCTile<TileFloodGate> implements IWrenchable {
    public static final Map<Direction, BooleanProperty> OPEN = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            if (side != Direction.UP) {
                OPEN.put(side, BooleanProperty.create("open_" + side.getSerializedName()));
            }
        }
    }

    public BlockFloodGate(Properties properties, Supplier<BlockEntityType<TileFloodGate>> tileType) {
        super(properties, tileType);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : OPEN.values()) {
            state = state.setValue(property, true);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        OPEN.values().forEach(builder::add);
    }

    public static boolean isOpen(BlockState state, Direction side) {
        BooleanProperty property = OPEN.get(side);
        return property != null && state.hasProperty(property) && state.getValue(property);
    }

    @Override
    public InteractionResult onWrench(BlockState state, Level level, BlockPos pos, Player player, Direction side) {
        BooleanProperty property = OPEN.get(side);
        if (property == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(property), Block.UPDATE_ALL);
            TileFloodGate tile = getTile(level, pos);
            if (tile != null) {
                tile.onSidesChanged();
            }
        }
        return InteractionResult.SUCCESS;
    }
}
