/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import net.minecraft.core.Direction;

/** A trigger that looks at one side of the container. */
public interface ITriggerInternalSided extends IStatement {
    boolean isTriggerActive(Direction side, IStatementContainer source, IStatementParameter[] parameters);
}
