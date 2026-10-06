/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import buildcraft.lib.misc.ZonePlan;

/** What a map location item stores.
 * @param positions For a spot, the block; for an area, its two corners; for a path, the positions along it.
 * @param side For a spot, the face of the block that was clicked. */
public record MapLocation(Type type, List<BlockPos> positions, Direction side, ZonePlan zone) {
    public enum Type implements StringRepresentable {
        SPOT, AREA, PATH, PATH_REPEATING, ZONE;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Type::ordinal);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<MapLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Type.CODEC.fieldOf("type").forGetter(MapLocation::type),
        BlockPos.CODEC.listOf().optionalFieldOf("positions", List.of()).forGetter(MapLocation::positions),
        Direction.CODEC.optionalFieldOf("side", Direction.UP).forGetter(MapLocation::side),
        ZonePlan.CODEC.optionalFieldOf("zone", ZonePlan.EMPTY).forGetter(MapLocation::zone)).apply(instance, MapLocation::new));
    public static final StreamCodec<ByteBuf, MapLocation> STREAM_CODEC = StreamCodec.composite(
        Type.STREAM_CODEC, MapLocation::type, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), MapLocation::positions,
        Direction.STREAM_CODEC, MapLocation::side, ZonePlan.STREAM_CODEC, MapLocation::zone, MapLocation::new);

    public static MapLocation spot(BlockPos pos, Direction side) {
        return new MapLocation(Type.SPOT, List.of(pos), side, ZonePlan.EMPTY);
    }

    public static MapLocation area(BlockPos min, BlockPos max) {
        return new MapLocation(Type.AREA, List.of(min, max), Direction.UP, ZonePlan.EMPTY);
    }

    public static MapLocation path(List<BlockPos> path) {
        boolean loops = path.size() > 2 && path.getFirst().equals(path.getLast());
        return new MapLocation(loops ? Type.PATH_REPEATING : Type.PATH, List.copyOf(path), Direction.UP, ZonePlan.EMPTY);
    }

    public static MapLocation zone(ZonePlan zone) {
        return new MapLocation(Type.ZONE, List.of(), Direction.UP, zone);
    }
}
