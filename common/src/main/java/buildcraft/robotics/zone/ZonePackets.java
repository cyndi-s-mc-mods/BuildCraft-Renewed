/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import buildcraft.lib.net.BCNetwork;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.tile.TileZonePlanner;

/** The packets the zone planner's GUI uses: asking the server for the map of some chunks, the map it sends back, and
 * painting part of a zone. */
public final class ZonePackets {
    /** The client asks for the map colours of the given chunks (as {@link net.minecraft.world.level.ChunkPos#pack}ed
     * longs) around the zone planner it has open. */
    public record MapRequest(BlockPos planner, List<Long> chunks) implements CustomPacketPayload {
        public static final Type<MapRequest> TYPE = BCNetwork.type("zone_map_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, MapRequest> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MapRequest::planner, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(64)), MapRequest::chunks,
            MapRequest::new);

        @Override
        public Type<MapRequest> type() {
            return TYPE;
        }
    }

    /** The map colours (ARGB) of the 256 columns of a chunk, x first. */
    public record MapData(long chunk, int[] colours) implements CustomPacketPayload {
        public static final Type<MapData> TYPE = BCNetwork.type("zone_map_data");
        private static final StreamCodec<ByteBuf, int[]> COLOURS = new StreamCodec<>() {
            @Override
            public int[] decode(ByteBuf buf) {
                int[] colours = new int[256];
                for (int i = 0; i < 256; i++) {
                    colours[i] = buf.readInt();
                }
                return colours;
            }

            @Override
            public void encode(ByteBuf buf, int[] colours) {
                for (int i = 0; i < 256; i++) {
                    buf.writeInt(colours[i]);
                }
            }
        };
        public static final StreamCodec<RegistryFriendlyByteBuf, MapData> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MapData::chunk, COLOURS, MapData::colours, MapData::new);

        @Override
        public Type<MapData> type() {
            return TYPE;
        }
    }

    /** Adds the columns from (x0, z0) to (x1, z1) to (or removes them from) the zone of one colour. */
    public record Paint(BlockPos planner, int colour, int x0, int z0, int x1, int z1, boolean add) implements CustomPacketPayload {
        public static final Type<Paint> TYPE = BCNetwork.type("zone_paint");
        public static final StreamCodec<RegistryFriendlyByteBuf, Paint> CODEC = new StreamCodec<>() {
            @Override
            public Paint decode(RegistryFriendlyByteBuf buf) {
                return new Paint(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Paint paint) {
                buf.writeBlockPos(paint.planner);
                buf.writeVarInt(paint.colour);
                buf.writeVarInt(paint.x0);
                buf.writeVarInt(paint.z0);
                buf.writeVarInt(paint.x1);
                buf.writeVarInt(paint.z1);
                buf.writeBoolean(paint.add);
            }
        };

        @Override
        public Type<Paint> type() {
            return TYPE;
        }
    }

    private ZonePackets() {}

    public static void init() {
        BCNetwork.toServer(MapRequest.TYPE, MapRequest.CODEC, (request, player) -> {
            if (player.containerMenu instanceof ContainerZonePlanner menu && menu.tile != null
                && menu.tile.getBlockPos().equals(request.planner())) {
                menu.tile.queueMapRequests(player, request.chunks());
            }
        });
        BCNetwork.toServer(Paint.TYPE, Paint.CODEC, (paint, player) -> {
            if (player.containerMenu instanceof ContainerZonePlanner menu && menu.tile != null
                && menu.tile.getBlockPos().equals(paint.planner()) && TileZonePlanner.canPaint(menu.getCarried(), paint.colour())) {
                menu.tile.paint(paint.colour(), paint.x0(), paint.z0(), paint.x1(), paint.z1(), paint.add());
            }
        });
        BCNetwork.toClient(MapData.TYPE, MapData.CODEC);
    }
}
