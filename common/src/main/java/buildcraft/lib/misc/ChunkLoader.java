/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

/** Keeps the chunks a machine works in loaded (like the /forceload command, but with its own tickets, so it doesn't undo
 * the player's). The tickets are saved with the world, and are renewed now and then while the machine runs. */
public final class ChunkLoader {
    /** Radius 2 gives the same ticket level as /forceload: the chunk is fully ticked. */
    private static final int RADIUS = 2;
    private static final int RENEW_INTERVAL = 200;

    public static RegistryEntry<TicketType, TicketType> TICKET;

    private final Set<ChunkPos> loaded = new HashSet<>();
    private int timer = 0;

    public static void init() {
        TICKET = BCRegistry.register(Registries.TICKET_TYPE, "machine", key -> new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    }

    /** @return The chunks with the machine and its area in. */
    public static Set<ChunkPos> chunksFor(BlockPos machine, @Nullable BoundingBox box) {
        Set<ChunkPos> chunks = new HashSet<>();
        chunks.add(ChunkPos.containing(machine));
        if (box != null) {
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    chunks.add(new ChunkPos(cx, cz));
                }
            }
        }
        return chunks;
    }

    /** Call every tick (on the server) with the chunks that should be loaded, or an empty set for none. */
    public void tick(ServerLevel level, Set<ChunkPos> wanted, boolean enabled) {
        if (!enabled) wanted = Set.of();
        if (timer-- > 0 && wanted.equals(loaded)) return;
        timer = RENEW_INTERVAL;
        for (ChunkPos pos : loaded) {
            if (!wanted.contains(pos)) {
                level.getChunkSource().removeTicketWithRadius(TICKET.get(), pos, RADIUS);
            }
        }
        // Adding a ticket that's already there does nothing, so renewing them is safe
        for (ChunkPos pos : wanted) {
            level.getChunkSource().addTicketWithRadius(TICKET.get(), pos, RADIUS);
        }
        loaded.clear();
        loaded.addAll(wanted);
    }

    /** Call when the machine is removed. */
    public void releaseAll(ServerLevel level) {
        for (ChunkPos pos : loaded) {
            level.getChunkSource().removeTicketWithRadius(TICKET.get(), pos, RADIUS);
        }
        loaded.clear();
    }
}
