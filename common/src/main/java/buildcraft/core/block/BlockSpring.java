/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.enums.EnumSpring;

/** An unbreakable block, generated near the bottom of the world, that keeps filling the space above it with water (or
 * oil). */
public class BlockSpring extends Block {
    public static final EnumProperty<EnumSpring> TYPE = EnumProperty.create("type", EnumSpring.class);

    public BlockSpring(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(TYPE, EnumSpring.WATER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, state.getValue(TYPE).tickRate);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        EnumSpring spring = state.getValue(TYPE);
        level.scheduleTick(pos, this, spring.tickRate);
        Supplier<BlockState> liquid = spring.liquidBlock;
        if (liquid == null || !level.isEmptyBlock(pos.above())) return;
        if (spring.chance != -1 && random.nextInt(spring.chance) != 0) return;
        level.setBlockAndUpdate(pos.above(), liquid.get());
    }

    /** Springs placed by world generation don't have a tick scheduled, so random ticks start them off. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, state.getValue(TYPE).tickRate);
        }
    }
}
