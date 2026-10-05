/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.tile.TileFiller;

/** A shape that a filler builds. Patterns are also gate actions, which set the pattern of a filler next to the gate. */
public abstract class Pattern implements IActionExternal {
    private final String name;
    private final Identifier icon;

    protected Pattern(String name) {
        this.name = name;
        this.icon = BuildCraft.id("textures/gui/filler/patterns/" + name + ".png");
    }

    /** Sets the positions in the template that should be filled.
     * @return False if this pattern doesn't build anything. */
    public abstract boolean fillTemplate(FilledTemplate template, IStatementParameter[] params);

    @Override
    public String getUniqueTag() {
        return BuildCraft.MOD_ID + ":filler_" + name;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fillerpattern." + name);
    }

    @Override
    public Identifier getIcon() {
        return icon;
    }

    @Override
    public void actionActivate(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
        if (target instanceof TileFiller filler) {
            filler.setPatternFromGate(this, parameters);
        }
    }

    /** @return The parameter at the given index, or the default if it's missing or the wrong type. */
    @SuppressWarnings("unchecked")
    protected static <P extends IStatementParameter> P getParam(int index, IStatementParameter[] params, P fallback) {
        if (index < params.length) {
            @Nullable IStatementParameter param = params[index];
            if (param != null && param.getClass() == fallback.getClass()) {
                return (P) param;
            }
        }
        return fallback;
    }
}
