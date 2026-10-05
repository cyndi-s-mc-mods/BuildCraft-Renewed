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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.client.render.LaserRenderer;

/** Draws the quarry's drill and the arms holding it, or the outline of the frame while it's being built. */
public class RenderQuarry implements BlockEntityRenderer<TileQuarry, RenderQuarry.State> {
    private static final SpriteId FRAME = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/frame/default"));
    private static final SpriteId DRILL = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/quarry/drill"));
    private static final SpriteId OUTLINE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/stripes_write"));
    private static final SpriteId LASER = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/power_low"));

    public static class State extends BlockEntityRenderState {
        @Nullable BoundingBox frame;
        @Nullable Vec3 drill;
        @Nullable Vec3 breaking;
        double drillOffset;
    }

    private final SpriteGetter sprites;

    public RenderQuarry(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileQuarry tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.frame = tile.getFrameBox();
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        Vec3 drill = tile.clientDrillPos;
        Vec3 prev = tile.prevClientDrillPos;
        state.drill = drill == null ? null : (prev == null ? drill : prev.lerp(drill, partialTicks)).subtract(origin);
        state.breaking = null;
        state.drillOffset = 1 + 4 / 16.0;
        if (tile.getCurrentTask() instanceof TileQuarry.TaskBreakBlock task) {
            if (drill == null) {
                if (task.power > 0) {
                    state.breaking = Vec3.atCenterOf(task.breakPos).subtract(origin);
                }
            } else {
                // The drill pulls back up as it finishes breaking each block
                double value = task.getTarget() <= 0 ? 0 : task.power / (double) task.getTarget();
                value = value < 0.9 ? 1 - value / 0.9 : (value - 0.9) / 0.1;
                state.drillOffset = 0.5 + value * (1 + 4 / 16.0 - 0.5);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        BoundingBox box = state.frame;
        if (box == null) return;
        TextureAtlasSprite frame = sprites.get(FRAME);
        TextureAtlasSprite drillSprite = sprites.get(DRILL);
        TextureAtlasSprite outline = sprites.get(OUTLINE);
        TextureAtlasSprite laser = sprites.get(LASER);
        int light = LightCoordsUtil.FULL_BRIGHT;
        int armLight = state.lightCoords;
        Vec3 drill = state.drill;
        Vec3 breaking = state.breaking;
        // The frame box relative to the quarry
        double x0 = box.minX() - state.blockPos.getX(), y0 = box.minY() - state.blockPos.getY(), z0 = box.minZ() - state.blockPos.getZ();
        double x1 = box.maxX() - state.blockPos.getX(), y1 = box.maxY() - state.blockPos.getY(), z1 = box.maxZ() - state.blockPos.getZ();
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            if (drill != null) {
                double top = y1 + 0.5;
                double dx = drill.x + 0.5, dz = drill.z + 0.5;
                LaserRenderer.laser(pose, buf, new Vec3(dx, top, drill.z), new Vec3(dx, top, z1 + 0.75), 0.5f, frame, 4, 12, armLight, -1);
                LaserRenderer.laser(pose, buf, new Vec3(dx, top, drill.z), new Vec3(dx, top, z0 + 0.25), 0.5f, frame, 4, 12, armLight, -1);
                LaserRenderer.laser(pose, buf, new Vec3(drill.x, top, dz), new Vec3(x1 + 0.75, top, dz), 0.5f, frame, 4, 12, armLight, -1);
                LaserRenderer.laser(pose, buf, new Vec3(drill.x, top, dz), new Vec3(x0 + 0.25, top, dz), 0.5f, frame, 4, 12, armLight, -1);
                double drillTop = drill.y + 1 + state.drillOffset;
                LaserRenderer.laser(pose, buf, new Vec3(dx, drillTop, dz), new Vec3(dx, top, dz), 0.5f, frame, 4, 12, armLight, -1);
                LaserRenderer.laser(pose, buf, new Vec3(dx, drillTop, dz), new Vec3(dx, drill.y + state.drillOffset, dz), 0.25f,
                    drillSprite, 0, 4, armLight, -1);
            } else {
                // The outline of the frame that's being built
                double[][] corners = { { x0, x1 }, { y0, y1 }, { z0, z1 } };
                for (int axis = 0; axis < 3; axis++) {
                    for (int a = 0; a < 2; a++) {
                        for (int b = 0; b < 2; b++) {
                            double[] from = new double[3], to = new double[3];
                            int other1 = (axis + 1) % 3, other2 = (axis + 2) % 3;
                            from[axis] = corners[axis][0];
                            to[axis] = corners[axis][1] + 1;
                            from[other1] = to[other1] = corners[other1][a] + 0.5;
                            from[other2] = to[other2] = corners[other2][b] + 0.5;
                            LaserRenderer.laser(pose, buf, new Vec3(from[0], from[1], from[2]), new Vec3(to[0], to[1], to[2]), 1 / 16f,
                                outline, 0, 4, light, -1);
                        }
                    }
                }
                if (breaking != null) {
                    LaserRenderer.laser(pose, buf, new Vec3(0.5, 0.5, 0.5), breaking, 1 / 16f, laser, 0, 4, light, -1);
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
