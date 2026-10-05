/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;

import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.client.render.BoxRenderer.Face;

/** Draws pluggable model parts (in the pose's current space, without turning them to a side). */
public final class PlugPartRenderer {
    private PlugPartRenderer() {}

    public static void submit(List<PlugModelPart> model, SpriteGetter sprites, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        submitParts(model.stream().filter(p -> (p.colour() >>> 24) == 0xFF).toList(), sprites, light, poseStack, collector,
            RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        List<PlugModelPart> translucent = model.stream().filter(p -> (p.colour() >>> 24) != 0xFF).toList();
        submitParts(translucent, sprites, light, poseStack, collector, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    private static void submitParts(List<PlugModelPart> model, SpriteGetter sprites, int light, PoseStack poseStack,
        SubmitNodeCollector collector, RenderType type) {
        if (model.isEmpty()) return;
        collector.submitCustomGeometry(poseStack, type, (pose, buf) -> {
            for (PlugModelPart part : model) {
                PlugModelPart.Face[] src = part.faces();
                Face[] faces = new Face[6];
                for (int f = 0; f < 6; f++) {
                    PlugModelPart.Face face = src == null ? null : src[f];
                    if (face != null) {
                        TextureAtlasSprite sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, face.sprite()));
                        faces[f] = new Face(sprite, face.u0(), face.v0(), face.u1(), face.v1());
                    }
                }
                BoxRenderer.box(pose, buf, part.x0(), part.y0(), part.z0(), part.x1(), part.y1(), part.z1(), faces, light, part.colour());
            }
        });
    }
}
