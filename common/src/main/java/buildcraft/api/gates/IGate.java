/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.gates;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.transport.pipe.IPipeHolder;

/** A gate on a pipe. Gate statements get this as their container. */
public interface IGate extends IStatementContainer {
    Direction getSide();

    IPipeHolder getPipeHolder();

    /** @return The strongest redstone signal going into the pipe. */
    int getRedstoneInput();

    /** Sets the redstone signal the gate gives out this tick.
     * @param sideOnly If true, only out of the gate's side, otherwise out of every side of the pipe. */
    void setRedstoneOutput(int level, boolean sideOnly);

    /** Sends a signal along the pipe wires of the given colour this tick. */
    void emitWire(DyeColor colour);
}
