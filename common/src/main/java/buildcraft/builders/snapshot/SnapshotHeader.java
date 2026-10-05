/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What an item (or the electronic library) knows about a snapshot: its key in the {@link SnapshotStore}, and enough to
 * describe it. */
public record SnapshotHeader(UUID key, Snapshot.Type type, String name, List<Integer> size) {
    private static final Codec<Snapshot.Type> TYPE_CODEC = Codec.STRING.xmap(Snapshot.Type::valueOf, Snapshot.Type::name);

    public static final Codec<SnapshotHeader> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("key").forGetter(SnapshotHeader::key),
        TYPE_CODEC.fieldOf("type").forGetter(SnapshotHeader::type),
        Codec.STRING.optionalFieldOf("name", "").forGetter(SnapshotHeader::name),
        Codec.INT.listOf().fieldOf("size").forGetter(SnapshotHeader::size)).apply(i, SnapshotHeader::new));

    public static final StreamCodec<ByteBuf, SnapshotHeader> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, SnapshotHeader::key,
        ByteBufCodecs.VAR_INT.map(i -> Snapshot.Type.values()[i], Snapshot.Type::ordinal), SnapshotHeader::type,
        ByteBufCodecs.STRING_UTF8, SnapshotHeader::name,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SnapshotHeader::size,
        SnapshotHeader::new);

    public static SnapshotHeader of(UUID key, Snapshot snapshot, String name) {
        return new SnapshotHeader(key, snapshot.type, name, List.of(snapshot.sizeX, snapshot.sizeY, snapshot.sizeZ));
    }

    public String sizeText() {
        return size.size() == 3 ? size.get(0) + "x" + size.get(1) + "x" + size.get(2) : "?";
    }
}
