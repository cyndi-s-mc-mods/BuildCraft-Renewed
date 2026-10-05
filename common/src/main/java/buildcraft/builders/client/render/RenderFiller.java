/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.builders.tile.TileFiller;
import buildcraft.client.render.LaserRenderer;

/** Draws the outline of the filler's area. */
public class RenderFiller implements BlockEntityRenderer<TileFiller, RenderFiller.State> {
    private static final SpriteId OUTLINE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/stripes_write"));

    public static class State extends BlockEntityRenderState {
        @Nullable BoundingBox box;
    }

    private final SpriteGetter sprites;

    public RenderFiller(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileFiller tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.box = tile.getBox();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        BoundingBox box = state.box;
        if (box == null) return;
        TextureAtlasSprite outline = sprites.get(OUTLINE);
        double[][] corners = { { box.minX() - state.blockPos.getX(), box.maxX() + 1 - state.blockPos.getX() },
            { box.minY() - state.blockPos.getY(), box.maxY() + 1 - state.blockPos.getY() },
            { box.minZ() - state.blockPos.getZ(), box.maxZ() + 1 - state.blockPos.getZ() } };
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            for (int axis = 0; axis < 3; axis++) {
                int other1 = (axis + 1) % 3, other2 = (axis + 2) % 3;
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        double[] from = new double[3], to = new double[3];
                        from[axis] = corners[axis][0];
                        to[axis] = corners[axis][1];
                        from[other1] = to[other1] = corners[other1][a];
                        from[other2] = to[other2] = corners[other2][b];
                        LaserRenderer.laser(pose, buf, new Vec3(from[0], from[1], from[2]), new Vec3(to[0], to[1], to[2]), 1 / 16f,
                            outline, 0, 4, LightCoordsUtil.FULL_BRIGHT, -1);
                    }
                }
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
