/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Void pipes destroy everything that reaches their centre. */
public class PipeBehaviourVoid extends PipeBehaviour {
    public PipeBehaviourVoid(IPipe pipe) {
        super(pipe);
    }

    @PipeEventHandler
    public static void reachCentre(PipeEventItem.ReachCenter reachCenter) {
        reachCenter.getStack().setCount(0);
    }

    @PipeEventHandler
    public static void moveFluidToCentre(PipeEventFluid.OnMoveToCentre move) {
        int removed = 0;
        for (int amount : move.fluidEnteringCentre) {
            removed += amount;
        }
        Arrays.fill(move.fluidEnteringCentre, 0);
        Level level = move.holder.getPipeWorld();
        BlockPos pos = move.holder.getPipePos();
        if (removed > 0 && (level.getGameTime() + pos.asLong()) % 23 == 0) {
            SoundEvent sound;
            if (move.fluid.isSameFluid(Fluids.LAVA)) {
                sound = SoundEvents.BUCKET_EMPTY_LAVA;
            } else if (move.fluid.isSameFluid(Fluids.WATER)) {
                sound = SoundEvents.BUCKET_EMPTY;
            } else {
                sound = SoundEvents.BOTTLE_FILL_DRAGONBREATH;
            }
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.4f, 0.1f);
        }
    }
}
