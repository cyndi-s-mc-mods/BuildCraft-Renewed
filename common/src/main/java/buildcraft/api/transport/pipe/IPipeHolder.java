/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
}
