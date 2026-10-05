/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.function.Function;

/** A kind of pipe. Each definition becomes its own block and item, named after {@link #id}. */
public final class PipeDefinition {
    public final String id;
    public final PipeFlowType flowType;
    public final Function<IPipe, PipeBehaviour> logic;
    public final boolean canBeColoured;

    public PipeDefinition(String id, PipeFlowType flowType, Function<IPipe, PipeBehaviour> logic, boolean canBeColoured) {
        this.id = id;
        this.flowType = flowType;
        this.logic = logic;
        this.canBeColoured = canBeColoured;
    }

    @Override
    public String toString() {
        return "PipeDefinition[" + id + "]";
    }
}
