/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.tiles.IAreaProvider;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.lib.tile.TileBC;

/** Volume markers connect to other markers in line with them (up to {@link #MAX_DISTANCE} blocks away) to mark out a
 * box, one connection along each axis. Right click a marker to connect it; power it with redstone to show where it can
 * connect. */
public class TileMarkerVolume extends TileBC implements IAreaProvider {
    public static final int MAX_DISTANCE = 64;

    /** The markers this one is directly connected to. */
    private final Set<BlockPos> connections = new LinkedHashSet<>();
    private boolean showSignals = false;

    public TileMarkerVolume(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.MARKER_VOLUME_TILE.get(), pos, state);
    }

    public boolean isShowingSignals() {
        return showSignals;
    }

    public Set<BlockPos> getConnections() {
        return connections;
    }

    public void setShowSignals(boolean show) {
        if (show != showSignals) {
            showSignals = show;
            setChanged();
            sendNetworkUpdate();
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (held.getItem() instanceof ItemMarkerConnector) {
            return ItemMarkerConnector.useOnMarker(held, player, this);
        }
        if (level != null && !level.isClientSide()) {
            tryConnect();
        }
        return InteractionResult.SUCCESS;
    }

    /** Connects to the nearest marker in each direction, as long as that doesn't connect two markers along the same
     * axis. */
    public void tryConnect() {
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            for (int i = 1; i <= MAX_DISTANCE; i++) {
                BlockPos other = worldPosition.relative(dir, i);
                if (level.getBlockEntity(other) instanceof TileMarkerVolume marker) {
                    if (canConnect(marker, dir.getAxis())) {
                        connect(marker);
                    }
                    break;
                }
            }
        }
    }

    /** Connects to the given marker, if it's in line with this one and close enough.
     * @return True if they were connected. */
    public boolean connectTo(TileMarkerVolume other) {
        BlockPos diff = other.worldPosition.subtract(worldPosition);
        int axes = (diff.getX() != 0 ? 1 : 0) + (diff.getY() != 0 ? 1 : 0) + (diff.getZ() != 0 ? 1 : 0);
        if (axes != 1 || diff.distManhattan(BlockPos.ZERO) > MAX_DISTANCE) return false;
        Direction.Axis axis = diff.getX() != 0 ? Direction.Axis.X : diff.getY() != 0 ? Direction.Axis.Y : Direction.Axis.Z;
        if (!canConnect(other, axis)) return false;
        connect(other);
        return true;
    }

    private boolean canConnect(TileMarkerVolume other, Direction.Axis axis) {
        if (connections.contains(other.worldPosition)) return false;
        List<TileMarkerVolume> ours = getGroup();
        if (ours.contains(other)) return false;
        Set<Direction.Axis> ourAxes = axes(ours);
        Set<Direction.Axis> theirAxes = axes(other.getGroup());
        if (ourAxes.contains(axis) || theirAxes.contains(axis)) return false;
        for (Direction.Axis a : ourAxes) {
            if (theirAxes.contains(a)) return false;
        }
        return true;
    }

    private void connect(TileMarkerVolume other) {
        connections.add(other.worldPosition);
        other.connections.add(worldPosition);
        setChanged();
        other.setChanged();
        sendNetworkUpdate();
        other.sendNetworkUpdate();
    }

    private static Set<Direction.Axis> axes(List<TileMarkerVolume> group) {
        Set<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
        for (TileMarkerVolume marker : group) {
            for (BlockPos other : marker.connections) {
                BlockPos diff = other.subtract(marker.worldPosition);
                if (diff.getX() != 0) axes.add(Direction.Axis.X);
                if (diff.getY() != 0) axes.add(Direction.Axis.Y);
                if (diff.getZ() != 0) axes.add(Direction.Axis.Z);
            }
        }
        return axes;
    }

    /** @return This marker and every marker connected to it, directly or not. */
    public List<TileMarkerVolume> getGroup() {
        List<TileMarkerVolume> group = new ArrayList<>();
        group.add(this);
        if (level == null) return group;
        Deque<TileMarkerVolume> open = new ArrayDeque<>(group);
        while (!open.isEmpty()) {
            TileMarkerVolume marker = open.removeFirst();
            for (BlockPos pos : marker.connections) {
                if (level.getBlockEntity(pos) instanceof TileMarkerVolume other && !group.contains(other)) {
                    group.add(other);
                    open.add(other);
                }
            }
        }
        return group;
    }

    public boolean hasConnection() {
        return !connections.isEmpty();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (BlockPos other : connections) {
            if (level.getBlockEntity(other) instanceof TileMarkerVolume marker && marker.connections.remove(worldPosition)) {
                marker.setChanged();
                marker.sendNetworkUpdate();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("connections", BlockPos.CODEC.listOf(), new ArrayList<>(connections));
        output.putBoolean("showSignals", showSignals);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        connections.clear();
        input.read("connections", BlockPos.CODEC.listOf()).ifPresent(connections::addAll);
        showSignals = input.getBooleanOr("showSignals", false);
    }

    // IAreaProvider

    @Override
    public BlockPos min() {
        BlockPos min = worldPosition;
        for (TileMarkerVolume marker : getGroup()) {
            min = BlockPos.min(min, marker.worldPosition);
        }
        return min;
    }

    @Override
    public BlockPos max() {
        BlockPos max = worldPosition;
        for (TileMarkerVolume marker : getGroup()) {
            max = BlockPos.max(max, marker.worldPosition);
        }
        return max;
    }

    @Override
    public void removeFromWorld() {
        if (level == null || level.isClientSide()) return;
        for (TileMarkerVolume marker : getGroup()) {
            level.destroyBlock(marker.worldPosition, true);
        }
    }

    @Override
    public boolean isValidFromLocation(BlockPos pos) {
        if (!hasConnection()) return false;
        BlockPos min = min(), max = max();
        boolean inside = pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY()
            && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        if (inside) return false;
        for (int x : new int[] { min.getX(), max.getX() }) {
            for (int y : new int[] { min.getY(), max.getY() }) {
                for (int z : new int[] { min.getZ(), max.getZ() }) {
                    if (pos.distManhattan(new BlockPos(x, y, z)) == 1) return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static IAreaProvider findAdjacent(net.minecraft.world.level.Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof IAreaProvider area && area.isValidFromLocation(pos)) {
                return area;
            }
        }
        return null;
    }
}
