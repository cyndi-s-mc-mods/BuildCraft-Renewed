/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;

/** A set of columns of the world (x and z positions), stored a chunk at a time. Painted in the zone planner and stored
 * on map locations. Immutable: the "with" methods return changed copies. */
public final class ZonePlan {
    public static final ZonePlan EMPTY = new ZonePlan(Map.of());

    /** One chunk's columns: bit (z * 16 + x) of the 256 bits is set for each column in the zone. */
    private record Chunk(int x, int z, List<Long> bits) {
        static final Codec<Chunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(Chunk::x),
            Codec.INT.fieldOf("z").forGetter(Chunk::z),
            Codec.LONG.listOf(4, 4).fieldOf("bits").forGetter(Chunk::bits)).apply(instance, Chunk::new));
        static final StreamCodec<ByteBuf, Chunk> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Chunk::x, ByteBufCodecs.VAR_INT, Chunk::z,
            ByteBufCodecs.LONG.apply(ByteBufCodecs.list(4)), Chunk::bits, Chunk::new);
    }

    public static final Codec<ZonePlan> CODEC = Chunk.CODEC.listOf().xmap(ZonePlan::fromChunks, ZonePlan::toChunks);
    public static final StreamCodec<ByteBuf, ZonePlan> STREAM_CODEC = Chunk.STREAM_CODEC.apply(ByteBufCodecs.list())
        .map(ZonePlan::fromChunks, ZonePlan::toChunks);

    private final Map<Long, long[]> chunks;

    private ZonePlan(Map<Long, long[]> chunks) {
        this.chunks = chunks;
    }

    private static ZonePlan fromChunks(List<Chunk> list) {
        Map<Long, long[]> chunks = new HashMap<>();
        for (Chunk chunk : list) {
            long[] bits = new long[4];
            for (int i = 0; i < 4; i++) {
                bits[i] = chunk.bits().get(i);
            }
            chunks.put(ChunkPos.pack(chunk.x(), chunk.z()), bits);
        }
        return new ZonePlan(chunks);
    }

    private List<Chunk> toChunks() {
        List<Chunk> list = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : chunks.entrySet()) {
            long[] bits = entry.getValue();
            list.add(new Chunk(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), List.of(bits[0], bits[1], bits[2], bits[3])));
        }
        return list;
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    public boolean get(int x, int z) {
        long[] bits = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        if (bits == null) return false;
        int bit = (z & 15) * 16 + (x & 15);
        return (bits[bit >> 6] & (1L << (bit & 63))) != 0;
    }

    /** @return A copy of this zone with the given columns (in the area from x0, z0 to x1, z1 inclusive) set or cleared. */
    public ZonePlan withArea(int x0, int z0, int x1, int z1, boolean value) {
        Map<Long, long[]> copy = new HashMap<>();
        chunks.forEach((key, bits) -> copy.put(key, bits.clone()));
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                long key = ChunkPos.pack(x >> 4, z >> 4);
                long[] bits = copy.get(key);
                if (bits == null) {
                    if (!value) continue;
                    bits = new long[4];
                    copy.put(key, bits);
                }
                int bit = (z & 15) * 16 + (x & 15);
                if (value) {
                    bits[bit >> 6] |= 1L << (bit & 63);
                } else {
                    bits[bit >> 6] &= ~(1L << (bit & 63));
                }
            }
        }
        copy.values().removeIf(bits -> bits[0] == 0 && bits[1] == 0 && bits[2] == 0 && bits[3] == 0);
        return new ZonePlan(copy);
    }

    /** @return The number of columns in the zone. */
    public int size() {
        int count = 0;
        for (long[] bits : chunks.values()) {
            for (long b : bits) {
                count += Long.bitCount(b);
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ZonePlan other) || other.chunks.size() != chunks.size()) return false;
        for (Map.Entry<Long, long[]> entry : chunks.entrySet()) {
            if (!java.util.Arrays.equals(entry.getValue(), other.chunks.get(entry.getKey()))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<Long, long[]> entry : chunks.entrySet()) {
            hash += Long.hashCode(entry.getKey()) ^ java.util.Arrays.hashCode(entry.getValue());
        }
        return hash;
    }
}
