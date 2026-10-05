/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

/** A trigger that looks at the container itself (for gates, the pipe). */
public interface ITriggerInternal extends IStatement {
    boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters);
}
