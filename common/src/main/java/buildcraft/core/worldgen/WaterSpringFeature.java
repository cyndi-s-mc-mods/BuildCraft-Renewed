/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import buildcraft.api.enums.EnumSpring;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreConfig;
import buildcraft.core.block.BlockSpring;

/** A water spring in the bedrock of about one chunk in forty, with a column of water above it up to the first open
 * space. */
public record WaterSpringFeature() implements Feature {
    public static final MapCodec<WaterSpringFeature> CODEC = MapCodec.unit(WaterSpringFeature::new);

    @Override
    public MapCodec<WaterSpringFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!BCCoreConfig.enableWaterSprings || random.nextFloat() > 0.025f) return false;
        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        // The highest bedrock in the floor of the world
        for (int y = level.getMinY() + 4; y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).is(Blocks.BEDROCK)) continue;
            placeSpring(level, pos, EnumSpring.WATER);
            for (BlockPos above = pos.above(); above.getY() < level.getMaxY() && !level.isEmptyBlock(above); above = above.above()) {
                level.setBlock(above, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return true;
        }
        return false;
    }

    /** Places a spring during world generation, and starts it off. */
    public static void placeSpring(WorldGenLevel level, BlockPos pos, EnumSpring type) {
        BlockState state = BCCoreBlocks.SPRING.get().defaultBlockState().setValue(BlockSpring.TYPE, type);
        if (level.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
            level.scheduleTick(pos, state.getBlock(), type.tickRate);
        }
    }
}
