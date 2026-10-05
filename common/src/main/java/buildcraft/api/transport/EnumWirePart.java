/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** The 8 places a pipe wire can go: one along each corner of the pipe. */
public enum EnumWirePart {
    EAST_UP_SOUTH(true, true, true),
    EAST_UP_NORTH(true, true, false),
    EAST_DOWN_SOUTH(true, false, true),
    EAST_DOWN_NORTH(true, false, false),
    WEST_UP_SOUTH(false, true, true),
    WEST_UP_NORTH(false, true, false),
    WEST_DOWN_SOUTH(false, false, true),
    WEST_DOWN_NORTH(false, false, false);

    public static final EnumWirePart[] VALUES = values();

    public final boolean x, y, z;
    public final String serialName = name().toLowerCase(Locale.ROOT);

    EnumWirePart(boolean x, boolean y, boolean z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static EnumWirePart get(boolean x, boolean y, boolean z) {
        return VALUES[(x ? 0 : 4) + (y ? 0 : 2) + (z ? 0 : 1)];
    }

    /** @return The part closest to the given point (relative to the block). */
    public static EnumWirePart closest(Vec3 local) {
        return get(local.x >= 0.5, local.y >= 0.5, local.z >= 0.5);
    }

    public boolean getSign(Direction.Axis axis) {
        return switch (axis) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    /** @return True if this part is on the given side of the pipe. */
    public boolean isOnSide(Direction side) {
        return getSign(side.getAxis()) == (side.getAxisDirection() == Direction.AxisDirection.POSITIVE);
    }

    /** @return The part on the other side along the given axis. */
    public EnumWirePart flip(Direction.Axis axis) {
        return get(axis == Direction.Axis.X ? !x : x, axis == Direction.Axis.Y ? !y : y, axis == Direction.Axis.Z ? !z : z);
    }

    public static EnumWirePart byName(String name) {
        for (EnumWirePart part : VALUES) {
            if (part.serialName.equals(name)) return part;
        }
        return EAST_UP_SOUTH;
    }
}
