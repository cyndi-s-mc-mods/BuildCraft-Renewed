/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** What a facade looks like: the block state it copies, and whether it has a hole in the middle for the pipe. */
public record FacadeData(BlockState state, boolean hollow) {
    public static final Codec<FacadeData> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockState.CODEC.fieldOf("state").forGetter(FacadeData::state),
        Codec.BOOL.optionalFieldOf("hollow", false).forGetter(FacadeData::hollow)).apply(i, FacadeData::new));

    public static final StreamCodec<ByteBuf, FacadeData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), FacadeData::state,
        ByteBufCodecs.BOOL, FacadeData::hollow,
        FacadeData::new);

    public FacadeData withHollow(boolean hollow) {
        return new FacadeData(state, hollow);
    }
}
