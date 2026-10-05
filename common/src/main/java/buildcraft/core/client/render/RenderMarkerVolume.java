package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.LightCoordsUtil;
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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.client.render.LaserRenderer;
import buildcraft.core.tile.TileMarkerVolume;

/** Draws the lasers between connected volume markers, and the signals of powered markers. */
public class RenderMarkerVolume implements BlockEntityRenderer<TileMarkerVolume, RenderMarkerVolume.State> {
    private static final SpriteId CONNECTED = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/marker_volume_connected"));
    private static final SpriteId SIGNAL = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/lasers/marker_volume_signal"));

    public static class State extends BlockEntityRenderState {
        final List<Vec3> connections = new ArrayList<>();
        boolean signals;
    }

    private final SpriteGetter sprites;

    public RenderMarkerVolume(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileMarkerVolume tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.connections.clear();
        BlockPos pos = tile.getBlockPos();
        for (BlockPos other : tile.getConnections()) {
            // Each connection is drawn once, by the marker with the lower position
            if (other.compareTo(pos) > 0) {
                state.connections.add(Vec3.atLowerCornerOf(other.subtract(pos)));
            }
        }
        state.signals = tile.isShowingSignals() && !tile.hasConnection();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.connections.isEmpty() && !state.signals) return;
        TextureAtlasSprite connected = sprites.get(CONNECTED);
        TextureAtlasSprite signal = sprites.get(SIGNAL);
        Vec3 centre = new Vec3(0.5, 0.5, 0.5);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            for (Vec3 offset : state.connections) {
                LaserRenderer.laser(pose, buf, centre, centre.add(offset), 2 / 16f, connected, 6, 8, LightCoordsUtil.FULL_BRIGHT, -1);
            }
            if (state.signals) {
                for (Direction dir : Direction.values()) {
                    Vec3 end = centre.add(Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(TileMarkerVolume.MAX_DISTANCE));
                    LaserRenderer.laser(pose, buf, centre, end, 1 / 16f, signal, 7, 8, LightCoordsUtil.FULL_BRIGHT, -1);
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
