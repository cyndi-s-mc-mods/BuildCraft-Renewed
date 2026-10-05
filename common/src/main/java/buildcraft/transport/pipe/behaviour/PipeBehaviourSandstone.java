/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Sandstone pipes only connect to other pipes. */
public class PipeBehaviourSandstone extends PipeBehaviour {
    public PipeBehaviourSandstone(IPipe pipe) {
        super(pipe);
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        return false;
    }

    @PipeEventHandler
    public static void modifySpeed(PipeEventItem.ModifySpeed event) {
        event.modifyTo(PipeBehaviourStone.SPEED_TARGET, PipeBehaviourStone.SPEED_DELTA);
    }
}
