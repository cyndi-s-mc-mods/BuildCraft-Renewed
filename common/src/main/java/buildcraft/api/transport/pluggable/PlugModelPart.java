/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pluggable;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

/** A box in a pluggable's model. Parts whose colour isn't fully opaque are drawn translucent. Its coordinates are in pixels (0-16), as it would be on the west side of the pipe. The renderer turns it
 * to face the pluggable's real side.
 * @param faces Indexed by {@link net.minecraft.core.Direction#get3DDataValue()}. Null faces aren't drawn. */
public record PlugModelPart(float x0, float y0, float z0, float x1, float y1, float z1, @Nullable Face[] faces, int colour) {
    /** @param sprite A sprite on the block atlas, such as "buildcraft:block/pipes/plug". */
    public record Face(Identifier sprite, float u0, float v0, float u1, float v1) {}

    public static PlugModelPart box(float x0, float y0, float z0, float x1, float y1, float z1, @Nullable Face[] faces) {
        return new PlugModelPart(x0, y0, z0, x1, y1, z1, faces, -1);
    }

    /** A box with the same sprite on every face, using the parts of the sprite that line up with the box. */
    public static PlugModelPart autoUv(float x0, float y0, float z0, float x1, float y1, float z1, Identifier sprite) {
        Face[] faces = new Face[6];
        faces[0] = new Face(sprite, x0, z0, x1, z1); // down
        faces[1] = new Face(sprite, x0, z0, x1, z1); // up
        faces[2] = new Face(sprite, 16 - x1, 16 - y1, 16 - x0, 16 - y0); // north
        faces[3] = new Face(sprite, x0, 16 - y1, x1, 16 - y0); // south
        faces[4] = new Face(sprite, z0, 16 - y1, z1, 16 - y0); // west
        faces[5] = new Face(sprite, 16 - z1, 16 - y1, 16 - z0, 16 - y0); // east
        return box(x0, y0, z0, x1, y1, z1, faces);
    }
}
