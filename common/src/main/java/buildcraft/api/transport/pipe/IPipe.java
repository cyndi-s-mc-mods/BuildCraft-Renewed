/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A pipe: a definition plus its behaviour (what kind of pipe it is) and flow (what it carries). */
public interface IPipe {
    IPipeHolder getHolder();

    PipeDefinition getDefinition();

    PipeBehaviour getBehaviour();

    PipeFlow getFlow();

    @Nullable
    DyeColor getColour();

    void setColour(@Nullable DyeColor colour);

    /** Makes the pipe recheck its connections next tick. */
    void markForUpdate();

    @Nullable
    BlockEntity getConnectedTile(Direction side);

    @Nullable
    IPipe getConnectedPipe(Direction side);

    boolean isConnected(Direction side);

    @Nullable
    ConnectedType getConnectedType(Direction side);

    enum ConnectedType {
        TILE,
        PIPE
    }
}
