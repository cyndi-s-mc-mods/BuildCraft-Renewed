/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.core.Direction;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;

/** Pipes of different materials that extend this don't connect to each other (such as stone and cobblestone). */
public abstract class PipeBehaviourSeparate extends PipeBehaviour {
    public PipeBehaviourSeparate(IPipe pipe) {
        super(pipe);
    }

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        if (other instanceof PipeBehaviourSeparate) {
            return other.getClass() == getClass();
        }
        return true;
    }
}
