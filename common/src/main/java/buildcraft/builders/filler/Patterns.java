/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.builders.filler.FillerParameters.Axis;
import buildcraft.builders.filler.FillerParameters.Center;
import buildcraft.builders.filler.FillerParameters.Facing;
import buildcraft.builders.filler.FillerParameters.Hollow;
import buildcraft.builders.filler.FillerParameters.Rotation;
import buildcraft.builders.filler.FillerParameters.XZDir;
import buildcraft.builders.filler.FillerParameters.YDir;

/** All the filler patterns. */
public final class Patterns {
    /** In the order they are shown in the filler's GUI. */
    public static final List<Pattern> ALL = new ArrayList<>();

    public static final Pattern NONE = add(new Pattern("none") {
        @Override
        public boolean fillTemplate(FilledTemplate template, IStatementParameter[] params) {
            return false;
        }
    });
    public static final Pattern CLEAR = add(new Pattern("clear") {
        @Override
        public boolean fillTemplate(FilledTemplate template, IStatementParameter[] params) {
            return true;
        }
    });
    public static final Pattern FILL = add(new Pattern("fill") {
        @Override
        public boolean fillTemplate(FilledTemplate template, IStatementParameter[] params) {
            template.setAll(true);
            return true;
        }
    });
    public static final Pattern BOX = add(new Pattern("box") {
        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            t.setPlaneYZ(0, true);
            t.setPlaneYZ(t.maxX(), true);
            t.setPlaneXZ(0, true);
            t.setPlaneXZ(t.maxY(), true);
            t.setPlaneXY(0, true);
            t.setPlaneXY(t.maxZ(), true);
            return true;
        }
    });
    public static final Pattern FRAME = add(new Pattern("frame") {
        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            int mx = t.maxX(), my = t.maxY(), mz = t.maxZ();
            t.setLineX(0, mx, 0, 0, true);
            t.setLineX(0, mx, my, 0, true);
            t.setLineX(0, mx, my, mz, true);
            t.setLineX(0, mx, 0, mz, true);
            t.setLineY(0, 0, my, 0, true);
            t.setLineY(mx, 0, my, 0, true);
            t.setLineY(mx, 0, my, mz, true);
            t.setLineY(0, 0, my, mz, true);
            t.setLineZ(0, 0, 0, mz, true);
            t.setLineZ(mx, 0, 0, mz, true);
            t.setLineZ(mx, my, 0, mz, true);
            t.setLineZ(0, my, 0, mz, true);
            return true;
        }
    });
    public static final Pattern PYRAMID = add(new Pattern("pyramid") {
        @Override
        public int minParameters() {
            return 2;
        }

        @Override
        public int maxParameters() {
            return 2;
        }

        @Override
        public @Nullable IStatementParameter createParameter(int index) {
            return switch (index) {
                case 0 -> YDir.UP;
                case 1 -> Center.CENTER;
                default -> null;
            };
        }

        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            Center center = getParam(1, params, Center.CENTER);
            int stepY = getParam(0, params, YDir.UP).up ? 1 : -1;
            int xLowerDiff = center.offsetX >= 0 ? 1 : 0, xUpperDiff = center.offsetX <= 0 ? -1 : 0;
            int zLowerDiff = center.offsetZ >= 0 ? 1 : 0, zUpperDiff = center.offsetZ <= 0 ? -1 : 0;
            int y = stepY == 1 ? 0 : t.maxY();
            int xLower = 0, xUpper = t.maxX(), zLower = 0, zUpper = t.maxZ();
            while (y >= 0 && y <= t.maxY()) {
                t.setAreaXZ(xLower, xUpper, y, zLower, zUpper, true);
                xLower += xLowerDiff;
                xUpper += xUpperDiff;
                zLower += zLowerDiff;
                zUpper += zUpperDiff;
                y += stepY;
                if (xLower > xUpper || zLower > zUpper) break;
            }
            return true;
        }
    });
    public static final Pattern STAIRS = add(new Pattern("stairs") {
        @Override
        public int minParameters() {
            return 2;
        }

        @Override
        public int maxParameters() {
            return 2;
        }

        @Override
        public @Nullable IStatementParameter createParameter(int index) {
            return switch (index) {
                case 0 -> YDir.UP;
                case 1 -> XZDir.EAST;
                default -> null;
            };
        }

        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            boolean up = getParam(0, params, YDir.UP).up;
            Direction dir = getParam(1, params, XZDir.EAST).dir;
            int y = up ? 0 : t.maxY();
            int yStep = up ? 1 : -1;
            int yEnd = up ? t.maxY() + 1 : -1;
            int fx = 0, fz = 0, tx = t.maxX(), tz = t.maxZ();
            while (y != yEnd) {
                t.setAreaXZ(fx, tx, y, fz, tz, true);
                fx += dir.getStepX() > 0 ? 1 : 0;
                fz += dir.getStepZ() > 0 ? 1 : 0;
                tx += dir.getStepX() < 0 ? -1 : 0;
                tz += dir.getStepZ() < 0 ? -1 : 0;
                y += yStep;
                if (fx > tx || fz > tz) break;
            }
            return true;
        }
    });
    public static final Pattern SPHERE = add(new PatternSphere("sphere", 0));
    public static final Pattern SPHERE_HALF = add(new PatternSphere("sphere_half", 1));
    public static final Pattern SPHERE_QUARTER = add(new PatternSphere("sphere_quarter", 2));
    public static final Pattern SPHERE_EIGHTH = add(new PatternSphere("sphere_eighth", 3));
    public static final Pattern SQUARE = add(new PatternShape2d("2d_square", (maxA, maxB, list) -> {
        list.lineTo(maxA, 0);
        list.lineTo(maxA, maxB);
        list.lineTo(0, maxB);
        list.lineTo(0, 0);
        list.setFillPoint(maxA / 2, maxB / 2);
    }));
    public static final Pattern CIRCLE = add(new PatternShape2d("2d_circle", (maxA, maxB, list) -> {
        if (maxA == 0 || maxB == 0) {
            list.lineTo(maxA, maxB);
            return;
        }
        int halfA = maxA / 2, halfB = maxB / 2;
        list.setFillPoint(halfA, halfB);
        list.arc(halfA, halfB, maxA / 2.0, maxB / 2.0, maxA - halfA - halfA, maxB - halfB - halfB, PatternShape2d.ArcType.FULL_CIRCLE);
    }));
    public static final Pattern SEMI_CIRCLE = add(new PatternShape2d("2d_semi_circle", (maxA, maxB, list) -> {
        if (maxA == 0 || maxB == 0) {
            list.lineTo(maxA, maxB);
            return;
        }
        int halfA = maxA / 2;
        list.setFillPoint(halfA, maxB);
        list.arc(halfA, maxB, maxA / 2.0, maxB, maxA - halfA - halfA, 0, PatternShape2d.ArcType.SEMI_CIRCLE);
    }));
    public static final Pattern ARC = add(new PatternShape2d("2d_arc", (maxA, maxB, list) -> {
        if (maxA == 0 || maxB == 0) {
            list.lineTo(maxA, maxB);
            return;
        }
        list.setFillPoint(maxA, maxB);
        list.arc(maxA, maxB, maxA, maxB, 0, 0, PatternShape2d.ArcType.ARC);
    }));
    public static final Pattern TRIANGLE = add(new PatternShape2d("2d_triangle", (maxA, maxB, list) -> {
        int halfA = maxA / 2;
        list.moveTo(maxA, maxB);
        list.lineTo(0, maxB);
        list.lineTo(halfA, 0);
        list.moveTo(maxA - halfA, 0);
        list.lineFrom(maxA, maxB);
        list.setFillPoint(halfA, maxB / 2);
    }));
    public static final Pattern PENTAGON = add(new PatternShape2d("2d_pentagon", (maxA, maxB, list) -> {
        double distHorizontal = Math.sin(Math.toRadians(108 - 90));
        double distVertical = Math.cos(Math.toRadians(54)) / Math.cos(Math.toRadians(18));
        int halfA = maxA / 2;
        int indentA = (int) Math.round(maxA * distHorizontal);
        int indentB = (int) Math.round(maxB * distVertical);
        list.moveTo(indentA, 0);
        list.lineTo(maxA - indentA, 0);
        list.lineFrom(maxA, indentB);
        list.lineTo(maxA - halfA, maxB);
        list.moveTo(halfA, maxB);
        list.lineFrom(0, indentB);
        list.lineTo(indentA, 0);
        list.setFillPoint(halfA, maxB / 2);
    }));
    public static final Pattern HEXAGON = add(new PatternShape2d("2d_hexagon", (maxA, maxB, list) -> {
        int indent = maxA / 4;
        int halfB = maxB / 2;
        list.moveTo(indent, 0);
        list.lineTo(maxA - indent, 0);
        list.lineFrom(maxA, halfB);
        list.moveTo(maxA, maxB - halfB);
        list.lineTo(maxA - indent, maxB);
        list.lineFrom(indent, maxB);
        list.lineFrom(0, maxB - halfB);
        list.moveTo(0, halfB);
        list.lineTo(indent, 0);
        list.setFillPoint(maxA / 2, halfB);
    }));
    public static final Pattern OCTAGON = add(new PatternShape2d("2d_octagon", (maxA, maxB, list) -> {
        int indentA = (int) Math.round(maxA / (2 + Math.sqrt(2)));
        int indentB = (int) Math.round(maxB / (2 + Math.sqrt(2)));
        indentA = Math.min(indentA, maxA / 2);
        indentB = Math.min(indentB, maxB / 2);
        list.moveTo(indentA, 0);
        list.lineTo(maxA - indentA, 0);
        list.lineTo(maxA, indentB);
        list.lineTo(maxA, maxB - indentB);
        list.lineTo(maxA - indentA, maxB);
        list.lineTo(indentA, maxB);
        list.lineTo(0, maxB - indentB);
        list.lineTo(0, indentB);
        list.lineTo(indentA, 0);
        list.setFillPoint(maxA / 2, maxB / 2);
    }));

    private Patterns() {}

    private static Pattern add(Pattern pattern) {
        ALL.add(pattern);
        return pattern;
    }

    public static void init() {
        for (Pattern pattern : ALL) {
            StatementManager.registerStatement(pattern);
        }
    }

    /** A sphere, or part of one (cut in half one, two or three times). */
    static final class PatternSphere extends Pattern {
        private final int openFaces;

        PatternSphere(String name, int openFaces) {
            super(name);
            this.openFaces = openFaces;
        }

        @Override
        public int minParameters() {
            return openFaces == 0 ? 1 : openFaces == 1 ? 2 : 3;
        }

        @Override
        public int maxParameters() {
            return minParameters();
        }

        @Override
        public @Nullable IStatementParameter createParameter(int index) {
            if (index >= minParameters()) return null;
            return switch (index) {
                case 0 -> Hollow.FILLED_INNER;
                case 1 -> Facing.DOWN;
                case 2 -> Rotation.NONE;
                default -> null;
            };
        }

        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            Hollow hollow = getParam(0, params, Hollow.FILLED_INNER);
            double[] center = { t.maxX() / 2.0, t.maxY() / 2.0, t.maxZ() / 2.0 };
            double[] radius = { center[0] + 0.5, center[1] + 0.5, center[2] + 0.5 };
            Set<Direction> innerSides = EnumSet.noneOf(Direction.class);
            if (openFaces > 0) {
                Direction facing = getParam(1, params, Facing.DOWN).face;
                int rotation = getParam(2, params, Rotation.NONE).rotationCount();
                Direction.Axis axis = facing.getAxis();
                cut(facing, center, radius, innerSides);
                if (openFaces > 1) {
                    cut(secondaryFace(axis, rotation), center, radius, innerSides);
                    if (openFaces > 2) {
                        cut(secondaryFace(axis, (rotation + 1) & 3), center, radius, innerSides);
                    }
                }
            }
            BitSet inside = new BitSet(t.sizeX * t.sizeY * t.sizeZ);
            for (int x = 0; x <= t.maxX(); x++) {
                double dx = Math.abs(x - center[0]) / radius[0];
                for (int y = 0; y <= t.maxY(); y++) {
                    double dy = Math.abs(y - center[1]) / radius[1];
                    for (int z = 0; z <= t.maxZ(); z++) {
                        double dz = Math.abs(z - center[2]) / radius[2];
                        if (dx * dx + dy * dy + dz * dz < 1) {
                            inside.set(t.index(x, y, z));
                        }
                    }
                }
            }
            if (hollow == Hollow.FILLED_INNER) {
                for (int i = inside.nextSetBit(0); i >= 0; i = inside.nextSetBit(i + 1)) {
                    int x = i % t.sizeX, z = (i / t.sizeX) % t.sizeZ, y = i / (t.sizeX * t.sizeZ);
                    t.set(x, y, z, true);
                }
                return true;
            }
            shell(t, inside, hollow.outerFilled, innerSides);
            return true;
        }

        private static Direction secondaryFace(Direction.Axis axis, int rotation) {
            Direction.Axis secondary;
            if (rotation % 2 == 1) {
                secondary = axis == Direction.Axis.X ? Direction.Axis.Y : axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.X;
            } else {
                secondary = axis == Direction.Axis.X ? Direction.Axis.Z : axis == Direction.Axis.Y ? Direction.Axis.X : Direction.Axis.Y;
            }
            return Direction.fromAxisAndDirection(secondary, rotation >= 2 ? Direction.AxisDirection.POSITIVE
                : Direction.AxisDirection.NEGATIVE);
        }

        /** Moves the centre of the sphere to the given side of the box, and doubles its radius on that axis. */
        private static void cut(Direction face, double[] center, double[] radius, Set<Direction> innerSides) {
            innerSides.add(face);
            int a = face.getAxis().ordinal();
            center[a] += face.getAxisDirection().getStep() * radius[a];
            radius[a] *= 2;
        }

        /** Keeps the outermost blocks of the shape along each axis (and everything outside it, if outerFilled), except
         * looking in from the cut sides. */
        private static void shell(FilledTemplate t, BitSet inside, boolean outerFilled, Set<Direction> innerSides) {
            int[] size = { t.sizeX, t.sizeY, t.sizeZ };
            for (Direction.Axis axis : Direction.Axis.values()) {
                int a = axis.ordinal(), b = (a + 1) % 3, c = (a + 2) % 3;
                for (int vb = 0; vb < size[b]; vb++) {
                    for (int vc = 0; vc < size[c]; vc++) {
                        for (int dir = 0; dir < 2; dir++) {
                            Direction from = Direction.fromAxisAndDirection(axis, dir == 0 ? Direction.AxisDirection.NEGATIVE
                                : Direction.AxisDirection.POSITIVE);
                            if (innerSides.contains(from)) continue;
                            for (int i = 0; i < size[a]; i++) {
                                int va = dir == 0 ? i : size[a] - 1 - i;
                                int[] p = new int[3];
                                p[a] = va;
                                p[b] = vb;
                                p[c] = vc;
                                if (inside.get(t.index(p[0], p[1], p[2]))) {
                                    t.set(p[0], p[1], p[2], true);
                                    break;
                                }
                                if (outerFilled) t.set(p[0], p[1], p[2], true);
                            }
                        }
                    }
                }
            }
        }
    }

    /** A 2D shape, drawn on a plane of the box and extended along the axis. */
    static final class PatternShape2d extends Pattern {
        interface ShapeGen {
            void gen(int maxA, int maxB, LineList list);
        }

        enum ArcType {
            ARC(false, false),
            SEMI_CIRCLE(true, false),
            FULL_CIRCLE(true, true);

            final boolean second, all;

            ArcType(boolean second, boolean all) {
                this.second = second;
                this.all = all;
            }
        }

        interface PathIterator2d {
            void iterate(int a, int b);
        }

        private final ShapeGen gen;

        PatternShape2d(String name, ShapeGen gen) {
            super(name);
            this.gen = gen;
        }

        @Override
        public int minParameters() {
            return 3;
        }

        @Override
        public int maxParameters() {
            return 3;
        }

        @Override
        public @Nullable IStatementParameter createParameter(int index) {
            return switch (index) {
                case 0 -> Axis.Y;
                case 1 -> Hollow.HOLLOW;
                case 2 -> Rotation.NONE;
                default -> null;
            };
        }

        @Override
        public boolean fillTemplate(FilledTemplate t, IStatementParameter[] params) {
            Axis axis = getParam(0, params, Axis.Y);
            int rotation = getParam(2, params, Rotation.NONE).rotationCount();
            PathIterator2d iterator = lineIterator(t, axis);
            int maxA = axis == Axis.X ? t.maxY() : t.maxX();
            int maxB = axis == Axis.Z ? t.maxY() : t.maxZ();
            int normMaxA = maxA, normMaxB = maxB;
            if (rotation % 2 == 1) {
                int tmp = maxA;
                maxA = maxB;
                maxB = tmp;
                final int fMaxB = maxB;
                final PathIterator2d old = iterator;
                iterator = (a, b) -> old.iterate(fMaxB - b, a);
            }
            if (rotation > 1) {
                final PathIterator2d old = iterator;
                final int fMaxA = maxA, fMaxB = maxB;
                iterator = (a, b) -> old.iterate(fMaxA - a, fMaxB - b);
            }
            LineList list = new LineList(iterator);
            gen.gen(maxA, maxB, list);
            Hollow filled = getParam(1, params, Hollow.HOLLOW);
            if (filled.filled && list.fillA != -1 && list.fillB != -1) {
                int fillA = list.fillA, fillB = list.fillB;
                maxA = normMaxA;
                maxB = normMaxB;
                if (rotation % 2 == 1) {
                    int tmp = fillA;
                    fillA = maxB - fillB;
                    fillB = tmp;
                }
                if (rotation > 1) {
                    fillA = maxA - fillA;
                    fillB = maxB - fillB;
                }
                PathIterator2d fill = filled.outerFilled ? (a, b) -> {} : lineIterator(t, axis);
                BiPredicate<Integer, Integer> isFilled = filledGetter(t, axis);
                // Flood outwards from the fill point, up to the outline
                BitSet visited = new BitSet((maxA + 1) * (maxB + 1));
                List<int[]> open = new ArrayList<>();
                open.add(new int[] { fillA, fillB });
                while (!open.isEmpty()) {
                    List<int[]> next = new ArrayList<>();
                    for (int[] p : open) {
                        if (p[0] < 0 || p[0] > maxA || p[1] < 0 || p[1] > maxB) continue;
                        int index = p[0] * (maxB + 1) + p[1];
                        if (visited.get(index)) continue;
                        visited.set(index);
                        if (isFilled.test(p[0], p[1])) continue;
                        fill.iterate(p[0], p[1]);
                        next.add(new int[] { p[0] + 1, p[1] });
                        next.add(new int[] { p[0] - 1, p[1] });
                        next.add(new int[] { p[0], p[1] + 1 });
                        next.add(new int[] { p[0], p[1] - 1 });
                    }
                    open = next;
                }
                if (filled.outerFilled) {
                    PathIterator2d line = lineIterator(t, axis);
                    for (int a = 0; a <= maxA; a++) {
                        for (int b = 0; b <= maxB; b++) {
                            if (!visited.get(a * (maxB + 1) + b)) line.iterate(a, b);
                        }
                    }
                }
            }
            return true;
        }

        private static PathIterator2d lineIterator(FilledTemplate t, Axis axis) {
            return switch (axis) {
                case X -> (y, z) -> t.setLineX(0, t.maxX(), y, z, true);
                case Y -> (x, z) -> t.setLineY(x, 0, t.maxY(), z, true);
                case Z -> (x, y) -> t.setLineZ(x, y, 0, t.maxZ(), true);
            };
        }

        private static BiPredicate<Integer, Integer> filledGetter(FilledTemplate t, Axis axis) {
            return switch (axis) {
                case X -> (a, b) -> t.get(0, a, b);
                case Y -> (a, b) -> t.get(a, 0, b);
                case Z -> (a, b) -> t.get(a, b, 0);
            };
        }
    }

    /** Draws lines and arcs for 2D shapes. */
    static final class LineList {
        private final PatternShape2d.PathIterator2d iterator;
        private int lastA, lastB;
        int fillA = -1, fillB = -1;

        LineList(PatternShape2d.PathIterator2d iterator) {
            this.iterator = iterator;
        }

        void setFillPoint(int a, int b) {
            fillA = a;
            fillB = b;
        }

        void moveTo(int a, int b) {
            lastA = a;
            lastB = b;
        }

        void lineTo(int a, int b) {
            line(lastA, lastB, a, b);
            moveTo(a, b);
        }

        void lineFrom(int a, int b) {
            int a2 = lastA, b2 = lastB;
            moveTo(a, b);
            lineTo(a2, b2);
            moveTo(a, b);
        }

        /** Bresenham's line, including both ends. */
        private void line(int a0, int b0, int a1, int b1) {
            int da = Math.abs(a1 - a0), db = -Math.abs(b1 - b0);
            int sa = a0 < a1 ? 1 : -1, sb = b0 < b1 ? 1 : -1;
            int err = da + db;
            while (true) {
                iterator.iterate(a0, b0);
                if (a0 == a1 && b0 == b1) break;
                int e2 = 2 * err;
                if (e2 >= db) {
                    err += db;
                    a0 += sa;
                }
                if (e2 <= da) {
                    err += da;
                    b0 += sb;
                }
            }
        }

        void arc(int ca, int cb, double ra, double rb, int da, int db, PatternShape2d.ArcType type) {
            double ra2 = ra * ra, rb2 = rb * rb;
            double sigma = 2 * rb2 + ra2 * (1 - 2 * rb);
            for (int a = 0, b = (int) rb; rb2 * a <= ra2 * b; a++) {
                plot(ca, cb, a, b, da, db, type);
                if (sigma >= 0) {
                    sigma += 4 * ra2 * (1 - b);
                    b--;
                }
                sigma += rb2 * ((4 * a) + 6);
            }
            sigma = 2 * ra2 + rb2 * (1 - 2 * ra);
            for (int a = (int) ra, b = 0; ra2 * b <= rb2 * a; b++) {
                plot(ca, cb, a, b, da, db, type);
                if (sigma >= 0) {
                    sigma += 4 * rb2 * (1 - a);
                    a--;
                }
                sigma += ra2 * ((4 * b) + 6);
            }
        }

        private void plot(int ca, int cb, int a, int b, int da, int db, PatternShape2d.ArcType type) {
            iterator.iterate(ca - a, cb - b);
            if (type.second) {
                iterator.iterate(ca + a + da, cb - b);
                if (type.all) {
                    iterator.iterate(ca - a, cb + b + db);
                    iterator.iterate(ca + a + da, cb + b + db);
                }
            }
        }
    }
}
