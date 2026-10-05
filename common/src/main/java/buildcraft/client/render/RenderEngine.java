package buildcraft.client.render;

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
import net.minecraft.core.Direction;

import buildcraft.BuildCraft;
import buildcraft.client.render.BoxRenderer.Face;
import buildcraft.lib.engine.TileEngineBase;

/** Draws the moving parts of an engine: the piston plate and the chamber below it. The rest is a block model. */
public class RenderEngine<T extends TileEngineBase> implements BlockEntityRenderer<T, RenderEngine.State> {
    public static class State extends BlockEntityRenderState {
        public float progress;
        public Direction facing = Direction.UP;
    }

    private final SpriteGetter sprites;
    private final SpriteId back, side, chamber;

    public RenderEngine(BlockEntityRendererProvider.Context context, String texture) {
        this.sprites = context.sprites();
        this.back = sprite("block/engine/" + texture + "/back");
        this.side = sprite("block/engine/" + texture + "/side");
        this.chamber = sprite("block/engine/chamber_base");
    }

    private static SpriteId sprite(String path) {
        return new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id(path));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T engine, State state, float partialTicks, net.minecraft.world.phys.Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(engine, state, partialTicks, cameraPosition, breakProgress);
        state.progress = engine.getProgressClient(partialTicks);
        state.facing = engine.getCurrentFacing();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float progress = state.progress;
        float offset = progress > 0.5f ? (1 - progress) * 15.99f : progress * 15.99f;

        TextureAtlasSprite backSprite = sprites.get(back);
        TextureAtlasSprite sideSprite = sprites.get(side);
        TextureAtlasSprite chamberSprite = sprites.get(chamber);

        Face backFace = Face.full(backSprite);
        Face sideFace = new Face(sideSprite, 0, 0, 16, 4);
        Face[] plate = { backFace, backFace, sideFace, sideFace, sideFace, sideFace };

        Face chamberFace = new Face(chamberSprite, 3, 0, 13, offset);
        Face[] chamberFaces = { null, null, chamberFace, chamberFace, chamberFace, chamberFace };

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.rotate(state.facing.getRotation());
        poseStack.translate(-0.5, -0.5, -0.5);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            BoxRenderer.box(pose, buf, 0, 4 + offset, 0, 16, 8 + offset, 16, plate, light, -1);
            if (offset > 0.01f) {
                BoxRenderer.box(pose, buf, 3, 4, 3, 13, 4 + offset, 13, chamberFaces, light, -1);
            }
        });
        poseStack.popPose();
    }
}
