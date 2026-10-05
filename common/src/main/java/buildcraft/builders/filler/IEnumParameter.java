/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

/** A filler pattern parameter with a fixed set of values, that clicking cycles through (left click forwards, right click
 * backwards). */
public interface IEnumParameter extends IStatementParameter {
    /** @return The values to cycle through, in order. */
    IEnumParameter[] cycle();

    int ordinal();

    /** @return The name of this value's icon, in textures/gui/filler/parameters. */
    String iconName();

    /** @return The translation key of this value's description. */
    String descriptionKey();

    @Override
    default ItemStack getItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    default Identifier getIcon() {
        return BuildCraft.id("textures/gui/filler/parameters/" + iconName() + ".png");
    }

    @Override
    default Component getDescription() {
        return Component.translatable(descriptionKey());
    }

    @Override
    default IStatementParameter onClick(IStatementContainer source, IStatement statement, ItemStack held, int button) {
        IEnumParameter[] values = cycle();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == this) index = i;
        }
        int next = (index + (button == 1 ? values.length - 1 : 1)) % values.length;
        return values[next];
    }

    @Override
    default void save(ValueOutput output) {
        output.putInt("v", ordinal());
    }
}
