/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.client.render.BoxRenderer;
import buildcraft.client.render.LaserRenderer;
import buildcraft.core.marker.VolumeBoxAddon;
import buildcraft.core.marker.VolumeBoxEntity;

/** Draws volume boxes: lasers along their edges, their addons on the corners, and the previews the addons show. */
public class RenderVolumeBox extends EntityRenderer<VolumeBoxEntity, RenderVolumeBox.State> {
    private static final SpriteId NORMAL = sprite("block/lasers/marker_volume_connected");
    private static final SpriteId EDITING = sprite("block/lasers/marker_volume_signal");
    private static final SpriteId READ = sprite("block/lasers/stripes_read");
    private static final SpriteId WRITE = sprite("block/lasers/stripes_write");
    private static final SpriteId WHITE = sprite("block/white");
    /** Previews of more blocks than this aren't drawn, as they would be too slow. */
    private static final int MAX_PREVIEW = 8192;

    public static class State extends EntityRenderState {
        AABB box = new AABB(0, 0, 0, 1, 1, 1);
        int lock;
        boolean editing;
        final Vec3[] corners = new Vec3[8];
        final Identifier[] addonSprites = new Identifier[8];
        /** The positions to preview, in the world. */
        final List<BlockPos> preview = new ArrayList<>();
        Vec3 origin = Vec3.ZERO;
    }

    private static SpriteId sprite(String path) {
        return new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id(path));
    }

    private final SpriteGetter sprites;

    public RenderVolumeBox(EntityRendererProvider.Context context) {
        super(context);
        this.sprites = context.getSprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VolumeBoxEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        state.origin = origin;
        state.box = entity.getBoundingBox().move(origin.scale(-1));
        state.lock = entity.getLock();
        state.editing = entity.isEditing();
        state.preview.clear();
        Level level = entity.level();
        for (int i = 0; i < 8; i++) {
            state.corners[i] = entity.getCorner(i).subtract(origin);
            VolumeBoxAddon addon = entity.getAddon(i);
            state.addonSprites[i] = addon == null ? null : addon.getSprite();
            boolean[] preview = addon == null || state.editing ? null : addon.getPreview(entity);
            if (preview == null) continue;
            BoundingBox box = entity.getBox();
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                int index = entity.indexOf(pos);
                if (index < preview.length && preview[index] && level.isEmptyBlock(pos)) {
                    state.preview.add(pos.immutable());
                    if (state.preview.size() >= MAX_PREVIEW) break;
                }
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite edge = sprites.get(state.editing ? EDITING
            : state.lock == VolumeBoxEntity.LOCK_READ ? READ : state.lock == VolumeBoxEntity.LOCK_WRITE ? WRITE : NORMAL);
        float v0 = state.lock != VolumeBoxEntity.LOCK_NONE ? 0 : state.editing ? 7 : 6;
        float v1 = state.lock != VolumeBoxEntity.LOCK_NONE ? 4 : 8;
        AABB box = state.box;
        double[][] corners = { { box.minX, box.maxX }, { box.minY, box.maxY }, { box.minZ, box.maxZ } };
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            for (int axis = 0; axis < 3; axis++) {
                int other1 = (axis + 1) % 3, other2 = (axis + 2) % 3;
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        double[] from = new double[3], to = new double[3];
                        from[axis] = corners[axis][0];
                        to[axis] = corners[axis][1];
                        from[other1] = to[other1] = corners[other1][a];
                        from[other2] = to[other2] = corners[other2][b];
                        LaserRenderer.laser(pose, buf, new Vec3(from[0], from[1], from[2]), new Vec3(to[0], to[1], to[2]), 1 / 16f, edge,
                            v0, v1, LightCoordsUtil.FULL_BRIGHT, -1);
                    }
                }
            }
            for (int i = 0; i < 8; i++) {
                Identifier sprite = state.addonSprites[i];
                if (sprite == null) continue;
                BoxRenderer.Face face = BoxRenderer.Face.full(sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, sprite)));
                Vec3 c = state.corners[i].scale(16);
                float r = (float) (VolumeBoxEntity.ADDON_RADIUS * 16);
                BoxRenderer.box(pose, buf, (float) c.x - r, (float) c.y - r, (float) c.z - r, (float) c.x + r, (float) c.y + r,
                    (float) c.z + r, new BoxRenderer.Face[] { face, face, face, face, face, face }, LightCoordsUtil.FULL_BRIGHT, -1);
            }
            if (!state.preview.isEmpty()) {
                BoxRenderer.Face white = BoxRenderer.Face.full(sprites.get(WHITE));
                BoxRenderer.Face[] faces = { white, white, white, white, white, white };
                for (BlockPos pos : state.preview) {
                    float x = (float) ((pos.getX() - state.origin.x) * 16), y = (float) ((pos.getY() - state.origin.y) * 16);
                    float z = (float) ((pos.getZ() - state.origin.z) * 16);
                    BoxRenderer.box(pose, buf, x + 2, y + 2, z + 2, x + 14, y + 14, z + 14, faces, LightCoordsUtil.FULL_BRIGHT, 0x60CCCCCC);
                }
            }
        });
    }

    @Override
    protected boolean affectedByCulling(VolumeBoxEntity entity) {
        // The local player sees the box while resizing it, even from inside
        var player = Minecraft.getInstance().player;
        return player == null || !entity.isEditedBy(player);
    }
}
