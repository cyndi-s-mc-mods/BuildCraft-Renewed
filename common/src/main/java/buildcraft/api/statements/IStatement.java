/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A trigger or action that can be put in a gate. */
public interface IStatement {
    /** @return A unique name, used when saving gates. Such as "buildcraft:redstone_input_active". */
    String getUniqueTag();

    Component getDescription();

    /** @return The icon shown in GUIs: a 16x16 texture, such as "buildcraft:textures/gui/triggers/trigger_true.png". */
    Identifier getIcon();

    default int minParameters() {
        return 0;
    }

    default int maxParameters() {
        return 0;
    }

    /** @return The default value of a parameter, or null if this statement has no parameter at that index. */
    @Nullable
    default IStatementParameter createParameter(int index) {
        return null;
    }
}
