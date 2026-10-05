package buildcraft.factory.client.render;

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
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import buildcraft.client.render.BoxRenderer;
import buildcraft.client.render.BoxRenderer.Face;
import buildcraft.factory.tile.TileTank;
import buildcraft.lib.client.FluidRenderUtil;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidStack;

/** Draws the fluid inside a tank. Tanks stacked on each other are drawn as one continuous column of fluid. */
public class RenderTank implements BlockEntityRenderer<TileTank, RenderTank.State> {
    private static final float MIN = 2.1f, MAX = 13.9f;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int colour = -1;
        float fill;
        boolean gaseous;
        boolean joinedBelow, joinedAbove;
    }

    public RenderTank(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileTank tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.sprite = null;
        BCFluidStack fluid = tile.tank.getFluid();
        float amount = tile.getFluidAmountForRender(partialTicks);
        if (fluid.isEmpty() || amount < 1) return;
        state.sprite = FluidRenderUtil.getStillSprite(fluid.getFluid());
        state.colour = FluidRenderUtil.getColour(fluid.getFluid());
        state.fill = Math.min(1, amount / tile.tank.getCapacity());
        state.gaseous = fluid.getFluid() instanceof BCFluid bc && bc.def.isGaseous();
        Level level = tile.getLevel();
        state.joinedBelow = level != null && hasSameFluid(level, tile, Direction.DOWN);
        state.joinedAbove = level != null && hasSameFluid(level, tile, Direction.UP);
    }

    private static boolean hasSameFluid(Level level, TileTank tile, Direction side) {
        return level.getBlockEntity(tile.getBlockPos().relative(side)) instanceof TileTank other
            && !other.tank.isEmpty() && other.tank.getFluid().isSameFluid(tile.tank.getFluid());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) return;
        float full = 16;
        float bottom = state.joinedBelow ? 0 : 0.1f;
        float top = state.joinedAbove && state.fill > 0.999f ? full : 15.9f;
        float height = (top - bottom) * state.fill;
        float y0 = state.gaseous ? top - height : bottom;
        float y1 = state.gaseous ? top : bottom + height;
        Face side = new Face(sprite, 0, 16 - (y1 - y0), 16, 16);
        Face end = Face.full(sprite);
        Face[] faces = { state.joinedBelow && !state.gaseous ? null : end, y1 >= full - 0.001f && state.joinedAbove ? null : end,
            side, side, side, side };
        int light = state.lightCoords;
        int colour = state.colour;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
            (pose, buf) -> BoxRenderer.box(pose, buf, MIN, y0, MIN, MAX, y1, MAX, faces, light, colour));
    }
}
