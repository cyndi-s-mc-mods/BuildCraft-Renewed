/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import net.minecraft.core.Direction;

/** Fired when a pipe connects or disconnects on a side. */
public class PipeEventConnectionChange extends PipeEvent {
    public final Direction side;

    public PipeEventConnectionChange(IPipeHolder holder, Direction side) {
        super(holder);
        this.side = side;
    }
}
