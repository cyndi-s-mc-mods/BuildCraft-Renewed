/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.lib.fluid.FluidUtilBC;

/** Diamond fluid pipes sort fluids by the buckets in their filters. */
public class PipeBehaviourDiamondFluid extends PipeBehaviourDiamond {
    public PipeBehaviourDiamondFluid(IPipe pipe) {
        super(pipe);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventFluid.SideCheck sideCheck) {
        for (Direction face : Direction.values()) {
            if (sideCheck.isAllowed(face) && pipe.isConnected(face)) {
                int offset = FILTERS_PER_SIDE * face.get3DDataValue();
                boolean sideAllowed = false;
                boolean foundItem = false;
                for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                    ItemStack compareTo = filters.getItem(offset + i);
                    if (compareTo.isEmpty()) continue;
                    Fluid target = FluidUtilBC.getBucketFluid(compareTo);
                    if (target == Fluids.EMPTY) {
                        continue;
                    }
                    foundItem = true;
                    if (sideCheck.fluid.isSameFluid(target)) {
                        sideAllowed = true;
                        break;
                    }
                }
                if (foundItem) {
                    if (sideAllowed) {
                        sideCheck.increasePriority(face, 12);
                    } else {
                        sideCheck.disallow(face);
                    }
                }
            }
        }
    }
}
