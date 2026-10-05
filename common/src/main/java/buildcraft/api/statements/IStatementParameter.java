/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;

/** A setting for a statement, such as the item an inventory trigger looks for. */
public interface IStatementParameter {
    /** @return The id this parameter type was registered with in {@link StatementManager#registerParameter}. */
    String getUniqueTag();

    /** @return The item to draw in the parameter's slot, or empty. */
    ItemStack getItemStack();

    /** @return A 16x16 texture to draw in the parameter's slot, or null. */
    @Nullable
    default Identifier getIcon() {
        return null;
    }

    Component getDescription();

    /** Called when a player clicks the parameter's slot.
     * @param held The item the player is holding on the cursor.
     * @param button 0 for left click, 1 for right click.
     * @return The new value of the parameter. */
    IStatementParameter onClick(IStatementContainer source, IStatement statement, ItemStack held, int button);

    void save(ValueOutput output);
}
