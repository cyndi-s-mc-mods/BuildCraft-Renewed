package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import buildcraft.client.render.BoxRenderer;
import buildcraft.client.render.BoxRenderer.Face;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.tile.TileDistiller;
import buildcraft.lib.client.FluidRenderUtil;
import buildcraft.lib.fluid.Tank;

/** Draws the fluid in the distiller's three tanks. */
public class RenderDistiller implements BlockEntityRenderer<TileDistiller, RenderDistiller.State> {
    /** The tanks' boxes (in pixels) when the distiller faces west: input, gas output, liquid output. */
    private static final float[][] BOXES = {
        { 0, 0, 4, 8, 16, 12 },
        { 8, 8, 0, 16, 16, 16 },
        { 8, 0, 0, 16, 8, 16 },
    };
    private static final float INSET = 0.25f;

    public static class State extends BlockEntityRenderState {
        final @Nullable TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
        final int[] colours = new int[3];
        final float[] fill = new float[3];
        float rotation;
    }

    public RenderDistiller(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileDistiller tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        Tank[] tanks = { tile.tankIn, tile.tankGasOut, tile.tankLiquidOut };
        for (int i = 0; i < 3; i++) {
            Tank tank = tanks[i];
            if (tank.isEmpty()) {
                state.sprites[i] = null;
                continue;
            }
            state.sprites[i] = FluidRenderUtil.getStillSprite(tank.getFluid().getFluid());
            state.colours[i] = FluidRenderUtil.getColour(tank.getFluid().getFluid());
            state.fill[i] = Math.min(1, tank.getFluidAmount() / (float) tank.getCapacity());
        }
        Direction facing = tile.getBlockState().hasProperty(BlockDistiller.FACING)
            ? tile.getBlockState().getValue(BlockDistiller.FACING) : Direction.WEST;
        // The boxes are for facing west; the block model is turned clockwise for the other directions
        state.rotation = switch (facing) {
            case NORTH -> 90;
            case EAST -> 180;
            case SOUTH -> 270;
            default -> 0;
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.rotate(Axis.YP.rotationDegrees(-state.rotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        int light = state.lightCoords;
        for (int i = 0; i < 3; i++) {
            TextureAtlasSprite sprite = state.sprites[i];
            if (sprite == null) continue;
            float[] b = BOXES[i];
            float y0 = b[1] + INSET;
            float y1 = y0 + (b[4] - b[1] - 2 * INSET) * state.fill[i];
            Face side = new Face(sprite, 0, 16 - (y1 - y0), 16, 16);
            Face end = new Face(sprite, b[0], b[2], b[3], b[5]);
            Face[] faces = { end, end, side, side, side, side };
            int colour = state.colours[i];
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                (pose, buf) -> BoxRenderer.box(pose, buf, b[0] + INSET, y0, b[2] + INSET, b[3] - INSET, y1, b[5] - INSET, faces, light,
                    colour));
        }
        poseStack.popPose();
    }
}
