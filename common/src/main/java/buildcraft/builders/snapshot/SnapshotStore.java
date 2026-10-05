/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import buildcraft.BuildCraft;

/** Keeps every snapshot that has been scanned, as a file each in the world's "buildcraft/snapshots" folder. Items only
 * hold the key of their snapshot, so that big blueprints don't have to be sent around with them. */
public final class SnapshotStore {
    private static final Map<UUID, Snapshot> CACHE = new ConcurrentHashMap<>();
    @Nullable
    private static MinecraftServer cachedFor;

    private SnapshotStore() {}

    private static Path folder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("buildcraft").resolve("snapshots");
    }

    private static void checkServer(MinecraftServer server) {
        if (cachedFor != server) {
            CACHE.clear();
            cachedFor = server;
        }
    }

    public static UUID add(MinecraftServer server, Snapshot snapshot) {
        checkServer(server);
        UUID key = UUID.randomUUID();
        CACHE.put(key, snapshot);
        try {
            Path folder = folder(server);
            Files.createDirectories(folder);
            NbtIo.writeCompressed(snapshot.save(), folder.resolve(key + ".nbt"));
        } catch (IOException e) {
            BuildCraft.LOGGER.error("Couldn't save snapshot {}", key, e);
        }
        return key;
    }

    @Nullable
    public static Snapshot get(MinecraftServer server, UUID key) {
        checkServer(server);
        Snapshot snapshot = CACHE.get(key);
        if (snapshot != null) return snapshot;
        Path file = folder(server).resolve(key + ".nbt");
        if (!Files.exists(file)) return null;
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            snapshot = Snapshot.load(tag);
            CACHE.put(key, snapshot);
            return snapshot;
        } catch (IOException | RuntimeException e) {
            BuildCraft.LOGGER.error("Couldn't load snapshot {}", key, e);
            return null;
        }
    }
}
