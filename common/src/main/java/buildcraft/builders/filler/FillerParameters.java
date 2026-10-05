/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;

/** The parameters of filler patterns. */
public final class FillerParameters {
    private FillerParameters() {}

    /** Every value of every parameter type, so a parameter can be sent to the client as a number. */
    public static final List<IEnumParameter> ALL = new ArrayList<>();

    public enum Axis implements IEnumParameter {
        X(Direction.Axis.X),
        Y(Direction.Axis.Y),
        Z(Direction.Axis.Z);

        public final Direction.Axis axis;

        Axis(Direction.Axis axis) {
            this.axis = axis;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_axis";
        }

        @Override
        public IEnumParameter[] cycle() {
            return values();
        }

        @Override
        public String iconName() {
            return "axis_" + name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String descriptionKey() {
            return "buildcraft.param.axis." + name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Center implements IEnumParameter {
        NORTH_WEST(-1, -1),
        NORTH(0, -1),
        NORTH_EAST(1, -1),
        WEST(-1, 0),
        CENTER(0, 0),
        EAST(1, 0),
        SOUTH_WEST(-1, 1),
        SOUTH(0, 1),
        SOUTH_EAST(1, 1);

        private static final Center[] ORDER = { CENTER, NORTH_WEST, NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST };

        public final int offsetX, offsetZ;

        Center(int x, int z) {
            offsetX = x;
            offsetZ = z;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_center";
        }

        @Override
        public IEnumParameter[] cycle() {
            return ORDER;
        }

        @Override
        public String iconName() {
            return "center_" + ordinal();
        }

        @Override
        public String descriptionKey() {
            return "direction.buildcraft.center." + ordinal();
        }
    }

    public enum Facing implements IEnumParameter {
        DOWN(Direction.DOWN),
        UP(Direction.UP),
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH),
        WEST(Direction.WEST),
        EAST(Direction.EAST);

        public final Direction face;

        Facing(Direction face) {
            this.face = face;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_facing";
        }

        @Override
        public IEnumParameter[] cycle() {
            return values();
        }

        @Override
        public String iconName() {
            return "face_" + face.getSerializedName();
        }

        @Override
        public String descriptionKey() {
            return "buildcraft.param.facing." + face.getSerializedName();
        }
    }

    public enum Hollow implements IEnumParameter {
        FILLED_INNER(true, false),
        FILLED_OUTER(true, true),
        HOLLOW(false, false);

        public final boolean filled;
        public final boolean outerFilled;

        Hollow(boolean filled, boolean outerFilled) {
            this.filled = filled;
            this.outerFilled = outerFilled;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_hollow";
        }

        @Override
        public IEnumParameter[] cycle() {
            return values();
        }

        @Override
        public String iconName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String descriptionKey() {
            return "fillerpattern.parameter." + (filled ? (outerFilled ? "filled_outer" : "filled") : "hollow");
        }
    }

    public enum Rotation implements IEnumParameter {
        NONE,
        QUARTER,
        HALF,
        THREE_QUARTERS;

        public int rotationCount() {
            return ordinal();
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_rotation";
        }

        @Override
        public IEnumParameter[] cycle() {
            return values();
        }

        @Override
        public String iconName() {
            return "rotation_" + ordinal();
        }

        @Override
        public String descriptionKey() {
            return "buildcraft.param.rotation." + ordinal();
        }
    }

    public enum XZDir implements IEnumParameter {
        WEST(Direction.WEST),
        EAST(Direction.EAST),
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH);

        private static final XZDir[] ORDER = { NORTH, EAST, SOUTH, WEST };

        public final Direction dir;

        XZDir(Direction dir) {
            this.dir = dir;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_xz_dir";
        }

        @Override
        public IEnumParameter[] cycle() {
            return ORDER;
        }

        @Override
        public String iconName() {
            return switch (this) {
                case WEST -> "arrow_left";
                case EAST -> "arrow_right";
                case NORTH -> "arrow_up";
                case SOUTH -> "arrow_down";
            };
        }

        @Override
        public String descriptionKey() {
            return "direction.buildcraft." + dir.getSerializedName();
        }
    }

    public enum YDir implements IEnumParameter {
        UP(true),
        DOWN(false);

        public final boolean up;

        YDir(boolean up) {
            this.up = up;
        }

        @Override
        public String getUniqueTag() {
            return "buildcraft:filler_parameter_y_dir";
        }

        @Override
        public IEnumParameter[] cycle() {
            return values();
        }

        @Override
        public String iconName() {
            return up ? "stairs_ascend" : "stairs_descend";
        }

        @Override
        public String descriptionKey() {
            return "direction.buildcraft." + (up ? "up" : "down");
        }
    }

    public static void init() {
        register(Axis.values());
        register(Center.values());
        register(Facing.values());
        register(Hollow.values());
        register(Rotation.values());
        register(XZDir.values());
        register(YDir.values());
    }

    private static void register(IEnumParameter[] values) {
        ALL.addAll(List.of(values));
        StatementManager.registerParameter(values[0].getUniqueTag(), input -> {
            int ordinal = input.getIntOr("v", 0);
            return values[ordinal >= 0 && ordinal < values.length ? ordinal : 0];
        });
    }

    /** @return The number used to send a parameter to the client: 0 for none. */
    public static int toId(@Nullable IStatementParameter param) {
        int index = param == null ? -1 : ALL.indexOf(param);
        return index + 1;
    }

    @Nullable
    public static IEnumParameter fromId(int id) {
        return id <= 0 || id > ALL.size() ? null : ALL.get(id - 1);
    }
}
