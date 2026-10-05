/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.function.Function;

public final class PipeFlowType {
    public final String name;
    public final Function<IPipe, PipeFlow> creator;

    public PipeFlowType(String name, Function<IPipe, PipeFlow> creator) {
        this.name = name;
        this.creator = creator;
    }
}
