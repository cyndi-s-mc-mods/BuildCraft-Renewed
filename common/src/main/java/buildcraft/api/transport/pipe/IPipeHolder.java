/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.pluggable.PipePluggable;

/** The block entity that holds a pipe. */
public interface IPipeHolder {
    Level getPipeWorld();

    BlockPos getPipePos();

    IPipe getPipe();

    @Nullable
    BlockEntity getNeighbourTile(Direction side);

    @Nullable
    IPipe getNeighbourPipe(Direction side);

    /** @return True if anything handled the event. */
    boolean fireEvent(PipeEvent event);

    /** Sends the pipe's state to clients at the end of the tick. */
    void scheduleNetworkUpdate();

    /** Asks for the block state to be recalculated (for connections and behaviour visuals). */
    void scheduleBlockStateUpdate();

    @Nullable
    PipePluggable getPluggable(Direction side);

    /** @return The colours of the wires in each wire position of this pipe. */
    Map<EnumWirePart, DyeColor> getWires();

    /** Adds a wire (null removes it). */
    void setWire(EnumWirePart part, @Nullable DyeColor colour);

    /** Replaces the pluggable on a side (null removes it).
     * @return The pluggable that was there before. */
    @Nullable
    PipePluggable replacePluggable(Direction side, @Nullable PipePluggable with);
}
