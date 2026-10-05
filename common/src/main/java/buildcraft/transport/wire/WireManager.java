/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

/** Works out which pipe wires carry a signal. Wires join up with wires of the same colour in the same pipe (along the
 * pipe's edges) and in the pipes next to them. A wire network carries a signal if any gate in a pipe on it is sending
 * one in that colour. Results are cached for the rest of the tick. */
public final class WireManager {
    private static final int MAX_NETWORK_SIZE = 4096;

    private record Node(BlockPos pos, EnumWirePart part) {}

    private static final class Cache {
        long tick = Long.MIN_VALUE;
        final Map<Node, Boolean> powered = new HashMap<>();
    }

    private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

    private WireManager() {}

    public static boolean isPowered(Level level, BlockPos pos, EnumWirePart part) {
        Cache cache = CACHES.computeIfAbsent(level, l -> new Cache());
        long tick = level.getGameTime();
        if (cache.tick != tick) {
            cache.tick = tick;
            cache.powered.clear();
        }
        Node start = new Node(pos.immutable(), part);
        Boolean known = cache.powered.get(start);
        if (known != null) return known;

        IPipeHolder startHolder = holder(level, pos);
        if (startHolder == null) return false;
        DyeColor colour = startHolder.getWires().get(part);
        if (colour == null) return false;

        List<Node> visited = new ArrayList<>();
        Set<Node> seen = new HashSet<>();
        Set<BlockPos> pipes = new HashSet<>();
        Deque<Node> open = new ArrayDeque<>();
        open.add(start);
        seen.add(start);
        boolean powered = false;
        while (!open.isEmpty() && visited.size() < MAX_NETWORK_SIZE) {
            Node node = open.removeFirst();
            visited.add(node);
            IPipeHolder holder = holder(level, node.pos);
            if (holder == null) continue;
            if (!powered && pipes.add(node.pos)) {
                for (Direction side : Direction.values()) {
                    PipePluggable plug = holder.getPluggable(side);
                    if (plug != null && plug.isEmittingWire(colour)) {
                        powered = true;
                        break;
                    }
                }
            }
            for (Node next : neighbours(level, node, holder, colour)) {
                if (seen.add(next)) open.add(next);
            }
        }
        for (Node node : visited) {
            cache.powered.put(node, powered);
        }
        return powered;
    }

    /** @return The wires (of the same colour) that this one joins up with. */
    private static List<Node> neighbours(Level level, Node node, IPipeHolder holder, DyeColor colour) {
        List<Node> list = new ArrayList<>();
        for (Direction.Axis axis : Direction.Axis.values()) {
            // Along the pipe's edge to the next corner
            EnumWirePart inner = node.part.flip(axis);
            if (holder.getWires().get(inner) == colour) {
                list.add(new Node(node.pos, inner));
            }
            // Across to the next pipe
            boolean positive = node.part.getSign(axis);
            Direction side = Direction.fromAxisAndDirection(axis, positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            BlockPos otherPos = node.pos.relative(side);
            IPipeHolder other = holder(level, otherPos);
            if (other != null && other.getWires().get(inner) == colour) {
                list.add(new Node(otherPos, inner));
            }
        }
        return list;
    }

    /** @return True if the wire at the given part connects to the pipe on the given side. Used for rendering. */
    public static boolean connectsAcross(Level level, BlockPos pos, EnumWirePart part, DyeColor colour, Direction side) {
        if (!part.isOnSide(side)) return false;
        IPipeHolder other = holder(level, pos.relative(side));
        return other != null && other.getWires().get(part.flip(side.getAxis())) == colour;
    }

    private static IPipeHolder holder(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof IPipeHolder holder ? holder : null;
    }
}
