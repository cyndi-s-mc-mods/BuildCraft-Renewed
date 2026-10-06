/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client.render;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.client.render.LaserRenderer;
import buildcraft.core.tile.TileMarkerPath;

/** Draws the laser from each path marker to the next one along its path. */
public class RenderMarkerPath implements BlockEntityRenderer<TileMarkerPath, RenderMarkerPath.State> {
    private static final SpriteId CONNECTED = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/marker_path_connected"));

    public static class State extends BlockEntityRenderState {
        @Nullable Vec3 next;
    }

    private final SpriteGetter sprites;

    public RenderMarkerPath(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileMarkerPath tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        BlockPos next = tile.getNext();
        state.next = next == null ? null : Vec3.atLowerCornerOf(next.subtract(tile.getBlockPos()));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 next = state.next;
        if (next == null) return;
        TextureAtlasSprite sprite = sprites.get(CONNECTED);
        Vec3 centre = new Vec3(0.5, 0.5, 0.5);
        Vec3 to = centre.add(next);
        // Start and stop just outside the markers, so the beam doesn't go through them
        Vec3 dir = to.subtract(centre).normalize().scale(0.125);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buf) ->
            LaserRenderer.laser(pose, buf, centre.add(dir), to.subtract(dir), 2 / 16f, sprite, 6, 8, LightCoordsUtil.FULL_BRIGHT, -1));
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
