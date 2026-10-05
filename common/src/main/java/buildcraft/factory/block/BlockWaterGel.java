/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;

/** Gel that spreads through still water, turning it solid. */
public class BlockWaterGel extends Block {
    public enum GelStage implements StringRepresentable {
        SPREAD_0(0.3f, true),
        SPREAD_1(0.4f, true),
        SPREAD_2(0.6f, true),
        SPREAD_3(0.8f, true),
        GELLING_0(1.0f, false),
        GELLING_1(1.2f, false),
        GEL(1.5f, false);

        public final SoundType soundType;
        public final boolean spreading;

        GelStage(float pitch, boolean spreading) {
            this.soundType = new SoundType(SoundType.SLIME_BLOCK.getVolume(), pitch, SoundEvents.SLIME_BLOCK_BREAK,
                SoundEvents.SLIME_BLOCK_STEP, SoundEvents.SLIME_BLOCK_PLACE, SoundEvents.SLIME_BLOCK_HIT, SoundEvents.SLIME_BLOCK_FALL);
            this.spreading = spreading;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public GelStage next() {
            return this == GEL ? GEL : values()[ordinal() + 1];
        }
    }

    public static final EnumProperty<GelStage> STAGE = EnumProperty.create("stage", GelStage.class);

    public BlockWaterGel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, GelStage.GEL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return state.getValue(STAGE).soundType;
    }

    /** Starts gel spreading from the given (water) position. */
    public void startSpreading(Level level, BlockPos pos) {
        level.setBlock(pos, defaultBlockState().setValue(STAGE, GelStage.SPREAD_0), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 200);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GelStage stage = state.getValue(STAGE);
        GelStage next = stage.next();
        BlockState nextState = state.setValue(STAGE, next);
        if (stage.spreading) {
            Deque<BlockPos> open = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            List<BlockPos> changeable = new ArrayList<>();
            List<Direction> faces = new ArrayList<>(List.of(Direction.values()));
            Collections.shuffle(faces);
            seen.add(pos);
            for (Direction face : faces) {
                open.add(pos.relative(face));
            }
            int tries = 0;
            while (!open.isEmpty() && changeable.size() < 3 && tries < 10_000) {
                BlockPos test = open.removeFirst();
                boolean water = isWater(level, test);
                if (water && level.getFluidState(test).isSource()) {
                    changeable.add(test);
                }
                if (water || level.getBlockState(test).is(this)) {
                    Collections.shuffle(faces);
                    for (Direction face : faces) {
                        BlockPos n = test.relative(face);
                        if (seen.add(n)) {
                            open.add(n);
                        }
                    }
                }
                tries++;
            }
            int time = next.spreading ? 200 : 400;
            if (changeable.size() == 3 || random.nextDouble() < 0.5) {
                for (BlockPos p : changeable) {
                    level.setBlock(p, nextState, Block.UPDATE_ALL);
                    level.scheduleTick(p, this, random.nextInt(150) + time);
                }
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                level.playSound(null, pos, next.soundType.getPlaceSound(), SoundSource.BLOCKS, 1, next.soundType.getPitch());
            }
            level.scheduleTick(pos, this, random.nextInt(150) + time);
        } else if (stage != next) {
            if (notTouchingWater(level, pos)) {
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                level.scheduleTick(pos, this, random.nextInt(150) + 400);
            } else {
                level.scheduleTick(pos, this, random.nextInt(150) + 600);
            }
        }
    }

    private static boolean notTouchingWater(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (isWater(level, pos.relative(face))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWater(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).is(Fluids.WATER);
    }
}
