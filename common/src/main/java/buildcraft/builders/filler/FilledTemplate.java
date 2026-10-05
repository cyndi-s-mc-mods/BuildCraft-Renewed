/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.BitSet;

/** Which positions in a box should be filled with blocks. Positions outside the box are ignored. */
public final class FilledTemplate {
    public final int sizeX, sizeY, sizeZ;
    private final BitSet data;

    public FilledTemplate(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.data = new BitSet(sizeX * sizeY * sizeZ);
    }

    public int maxX() {
        return sizeX - 1;
    }

    public int maxY() {
        return sizeY - 1;
    }

    public int maxZ() {
        return sizeZ - 1;
    }

    public int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public boolean get(int x, int y, int z) {
        return inside(x, y, z) && data.get(index(x, y, z));
    }

    public boolean get(int index) {
        return data.get(index);
    }

    public void set(int x, int y, int z, boolean value) {
        if (inside(x, y, z)) data.set(index(x, y, z), value);
    }

    public void invert() {
        data.flip(0, sizeX * sizeY * sizeZ);
    }

    public void setLineX(int fromX, int toX, int y, int z, boolean value) {
        for (int x = fromX; x <= toX; x++) set(x, y, z, value);
    }

    public void setLineY(int x, int fromY, int toY, int z, boolean value) {
        for (int y = fromY; y <= toY; y++) set(x, y, z, value);
    }

    public void setLineZ(int x, int y, int fromZ, int toZ, boolean value) {
        for (int z = fromZ; z <= toZ; z++) set(x, y, z, value);
    }

    public void setAreaYZ(int x, int fromY, int toY, int fromZ, int toZ, boolean value) {
        for (int y = fromY; y <= toY; y++) setLineZ(x, y, fromZ, toZ, value);
    }

    public void setAreaXZ(int fromX, int toX, int y, int fromZ, int toZ, boolean value) {
        for (int z = fromZ; z <= toZ; z++) setLineX(fromX, toX, y, z, value);
    }

    public void setAreaXY(int fromX, int toX, int fromY, int toY, int z, boolean value) {
        for (int y = fromY; y <= toY; y++) setLineX(fromX, toX, y, z, value);
    }

    public void setPlaneYZ(int x, boolean value) {
        setAreaYZ(x, 0, maxY(), 0, maxZ(), value);
    }

    public void setPlaneXZ(int y, boolean value) {
        setAreaXZ(0, maxX(), y, 0, maxZ(), value);
    }

    public void setPlaneXY(int z, boolean value) {
        setAreaXY(0, maxX(), 0, maxY(), z, value);
    }

    public void setAll(boolean value) {
        data.set(0, sizeX * sizeY * sizeZ, value);
    }
}
