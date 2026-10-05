/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.core;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** One of the six faces of a pipe, or its centre. */
public enum EnumPipePart implements StringRepresentable {
    DOWN(Direction.DOWN),
    UP(Direction.UP),
    NORTH(Direction.NORTH),
    SOUTH(Direction.SOUTH),
    WEST(Direction.WEST),
    EAST(Direction.EAST),
    CENTER(null);

    public static final EnumPipePart[] VALUES = values();
    public static final EnumPipePart[] FACES = { DOWN, UP, NORTH, SOUTH, WEST, EAST };

    public final @Nullable Direction face;
    private final String name = name().toLowerCase(Locale.ROOT);

    EnumPipePart(@Nullable Direction face) {
        this.face = face;
    }

    public int getIndex() {
        return ordinal();
    }

    public static EnumPipePart fromFacing(@Nullable Direction face) {
        return face == null ? CENTER : VALUES[face.get3DDataValue()];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
