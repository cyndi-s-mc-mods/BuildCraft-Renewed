/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** An action on a neighbouring block, such as turning a machine on or off. */
public interface IActionExternal extends IStatement {
    void actionActivate(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters);

    default void actionDeactivated(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {}
}
