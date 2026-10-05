/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import java.util.Collection;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IStatementProvider;
import buildcraft.api.statements.StatementManager;
import buildcraft.builders.filler.FillerParameters;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.builders.tile.TileFiller;

public final class BCBuildersStatements {
    private BCBuildersStatements() {}

    static void init() {
        FillerParameters.init();
        Patterns.init();
        StatementManager.registerProvider(new IStatementProvider() {
            @Override
            public void addExternalActions(Collection<IActionExternal> actions, Direction side, BlockEntity tile) {
                if (tile instanceof TileFiller) {
                    for (Pattern pattern : Patterns.ALL) {
                        actions.add(pattern);
                    }
                }
            }
        });
    }
}
