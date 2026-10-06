/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.IPathProvider;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.lib.tile.TileBC;

/** Path markers are joined one after another (with the marker connector) to make a path, which can loop back to its
 * start. The builder builds its blueprint at every step along a path. Right click a marker to reverse its path. */
public class TileMarkerPath extends TileBC implements IPathProvider {
    public static final int MAX_DISTANCE = 64;
    /** Stops a broken save from looping forever. */
    private static final int MAX_LENGTH = 4096;

    @Nullable
    private BlockPos next, prev;

    public TileMarkerPath(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.MARKER_PATH_TILE.get(), pos, state);
    }

    @Nullable
    public BlockPos getNext() {
        return next;
    }

    @Nullable
    private TileMarkerPath tileAt(@Nullable BlockPos pos) {
        return pos != null && level != null && level.getBlockEntity(pos) instanceof TileMarkerPath marker ? marker : null;
    }

    /** @return The first marker of this marker's path (or this marker, if the path loops). */
    private TileMarkerPath getHead() {
        TileMarkerPath head = this;
        for (int i = 0; i < MAX_LENGTH; i++) {
            TileMarkerPath before = tileAt(head.prev);
            if (before == null || before == this) return before == this ? this : head;
            head = before;
        }
        return head;
    }

    /** @return Every marker on this marker's path, in order. */
    public List<TileMarkerPath> getMarkers() {
        List<TileMarkerPath> markers = new ArrayList<>();
        TileMarkerPath marker = getHead();
        while (marker != null && !markers.contains(marker) && markers.size() < MAX_LENGTH) {
            markers.add(marker);
            marker = tileAt(marker.next);
        }
        return markers;
    }

    public boolean isLoop() {
        List<TileMarkerPath> markers = getMarkers();
        return markers.size() > 2 && markers.getFirst().worldPosition.equals(markers.getLast().next);
    }

    @Override
    public List<BlockPos> getPath() {
        List<TileMarkerPath> markers = getMarkers();
        if (markers.size() < 2) return List.of();
        List<BlockPos> path = new ArrayList<>();
        for (TileMarkerPath marker : markers) {
            path.add(marker.worldPosition);
        }
        if (isLoop()) path.add(path.getFirst());
        return path;
    }

    /** @return True if this marker can be followed by the given one. */
    private boolean canLinkTo(TileMarkerPath other) {
        if (other == this || next != null || other.prev != null) return false;
        if (worldPosition.distSqr(other.worldPosition) > MAX_DISTANCE * MAX_DISTANCE) return false;
        List<TileMarkerPath> ours = getMarkers();
        // Joining the end of a path to its start closes the loop, which needs at least three markers
        return !ours.contains(other) || ours.getFirst() == other && ours.size() > 2;
    }

    /** Joins two markers, in whichever order is possible (preferring this one first).
     * @return True if they were joined. */
    public boolean connectTo(TileMarkerPath other) {
        if (canLinkTo(other)) {
            link(this, other);
            return true;
        }
        if (other.canLinkTo(this)) {
            link(other, this);
            return true;
        }
        return false;
    }

    private static void link(TileMarkerPath from, TileMarkerPath to) {
        from.next = to.worldPosition;
        to.prev = from.worldPosition;
        from.changed();
        to.changed();
    }

    private void changed() {
        setChanged();
        sendNetworkUpdate();
    }

    /** Reverses the direction of this marker's path. */
    public void reverse() {
        for (TileMarkerPath marker : getMarkers()) {
            BlockPos oldNext = marker.next;
            marker.next = marker.prev;
            marker.prev = oldNext;
            marker.changed();
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (held.getItem() instanceof ItemMarkerConnector) {
            return ItemMarkerConnector.useOnMarker(held, player, this);
        }
        if (level != null && !level.isClientSide()) {
            reverse();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void removeFromWorld() {
        if (level == null || level.isClientSide()) return;
        for (TileMarkerPath marker : getMarkers()) {
            level.destroyBlock(marker.worldPosition, true);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        TileMarkerPath before = tileAt(prev), after = tileAt(next);
        if (before != null && worldPosition.equals(before.next)) {
            before.next = null;
            before.changed();
        }
        if (after != null && worldPosition.equals(after.prev)) {
            after.prev = null;
            after.changed();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("next", BlockPos.CODEC, next);
        output.storeNullable("prev", BlockPos.CODEC, prev);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        next = input.read("next", BlockPos.CODEC).orElse(null);
        prev = input.read("prev", BlockPos.CODEC).orElse(null);
    }
}
