/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import java.util.Collection;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Lists the triggers and actions that a container can use. Every method is optional. */
public interface IStatementProvider {
    default void addInternalTriggers(Collection<ITriggerInternal> triggers, IStatementContainer container) {}

    default void addInternalSidedTriggers(Collection<ITriggerInternalSided> triggers, IStatementContainer container, Direction side) {}

    /** @param side The side of the container that the tile is on. */
    default void addExternalTriggers(Collection<ITriggerExternal> triggers, Direction side, BlockEntity tile) {}

    default void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {}

    default void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container, Direction side) {}

    default void addExternalActions(Collection<IActionExternal> actions, Direction side, BlockEntity tile) {}
}
