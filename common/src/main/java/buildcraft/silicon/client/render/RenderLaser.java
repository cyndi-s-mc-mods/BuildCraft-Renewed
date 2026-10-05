package buildcraft.silicon.client.render;

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
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.api.mj.MjAPI;
import buildcraft.client.render.LaserRenderer;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.tile.TileLaser;

/** Draws the beam from a laser to the table it's powering. Its colour shows how much power it's sending. */
public class RenderLaser implements BlockEntityRenderer<TileLaser, RenderLaser.State> {
    private static final SpriteId[] POWERS = {
        new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/power_low")),
        new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/power_med")),
        new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/power_high")),
        new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/power_full")),
    };

    public static class State extends BlockEntityRenderState {
        @Nullable Vec3 from, to;
        int power;
    }

    private final SpriteGetter sprites;

    public RenderLaser(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileLaser tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.to = null;
        Vec3 target = tile.laserPos;
        if (target == null || tile.getTargetPos() == null) return;
        Direction facing = tile.getBlockState().hasProperty(BlockLaser.FACING) ? tile.getBlockState().getValue(BlockLaser.FACING)
            : Direction.UP;
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        state.from = new Vec3(0.5, 0.5, 0.5).add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(4 / 16.0));
        state.to = target.subtract(origin);
        double fraction = tile.getAveragePower() / (double) TileLaser.MAX_POWER_PER_TICK;
        state.power = fraction > 0.9 ? 3 : fraction > 0.6 ? 2 : fraction > 0.3 ? 1 : 0;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 from = state.from, to = state.to;
        if (from == null || to == null) return;
        var sprite = sprites.get(POWERS[state.power]);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
            (pose, buf) -> LaserRenderer.laser(pose, buf, from, to, 1 / 8f, sprite, 4, 12, LightCoordsUtil.FULL_BRIGHT, -1));
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
