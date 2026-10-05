package buildcraft.transport.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.client.render.BoxRenderer;
import buildcraft.client.render.BoxRenderer.Face;
import buildcraft.lib.client.FluidRenderUtil;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.tile.TilePipeHolder;

/** Draws what's moving through a pipe: items, fluid or power. The pipe itself is a block model. */
public class RenderPipeHolder implements BlockEntityRenderer<TilePipeHolder, RenderPipeHolder.State> {
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> items = new ArrayList<>();
        final List<Vec3> itemPositions = new ArrayList<>();
        @Nullable TextureAtlasSprite fluidSprite;
        int fluidColour = -1;
        boolean fluidGaseous;
        final double[] fluidAmounts = new double[7];
        int fluidCapacity;
        final boolean[] connected = new boolean[6];
        final List<Direction> plugSides = new ArrayList<>();
        final List<List<PlugModelPart>> plugModels = new ArrayList<>();
        final float[] power = new float[6];
        float centerPower;
    }

    private final ItemModelResolver itemModelResolver;
    private final SpriteGetter sprites;
    private static final SpriteId POWER_FLOW = new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/pipes/power_flow"));

    public RenderPipeHolder(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TilePipeHolder tile, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.items.clear();
        state.itemPositions.clear();
        state.plugSides.clear();
        state.plugModels.clear();
        for (Map.Entry<Direction, PipePluggable> entry : tile.getPluggables().entrySet()) {
            List<PlugModelPart> model = entry.getValue().getModel();
            if (!model.isEmpty()) {
                state.plugSides.add(entry.getKey());
                state.plugModels.add(model);
            }
        }
        state.fluidSprite = null;
        state.centerPower = 0;
        Level level = tile.getLevel();
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            state.connected[dir.get3DDataValue()] = tile.getPipe().isConnected(dir);
            state.power[dir.get3DDataValue()] = 0;
        }

        if (tile.getPipe().getFlow() instanceof PipeFlowItems flow) {
            long now = level.getGameTime();
            int seed = (int) tile.getBlockPos().asLong();
            for (TravellingItem item : flow.getAllItemsForRender()) {
                if (item.getStack().isEmpty()) continue;
                ItemStackRenderState itemState = new ItemStackRenderState();
                itemModelResolver.updateForTopItem(itemState, item.getStack(), ItemDisplayContext.GROUND, level, null, seed++);
                state.items.add(itemState);
                state.itemPositions.add(item.getRenderPosition(now, partialTicks, flow));
            }
        } else if (tile.getPipe().getFlow() instanceof PipeFlowFluids flow) {
            BCFluidStack fluid = flow.getFluidForRender();
            if (!fluid.isEmpty()) {
                state.fluidSprite = FluidRenderUtil.getStillSprite(fluid.getFluid());
                state.fluidColour = FluidRenderUtil.getColour(fluid.getFluid());
                state.fluidCapacity = flow.capacity;
                double[] amounts = flow.getAmountsForRender(partialTicks);
                System.arraycopy(amounts, 0, state.fluidAmounts, 0, 7);
            }
        } else if (tile.getPipe().getFlow() instanceof PipeFlowPower flow) {
            float max = 0;
            for (Direction dir : Direction.values()) {
                float p = flow.getDisplayPower(dir) / (float) buildcraft.api.mj.MjAPI.MJ;
                state.power[dir.get3DDataValue()] = p;
                max = Math.max(max, p);
            }
            state.centerPower = max;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.plugSides.size(); i++) {
            submitPluggable(state.plugSides.get(i), state.plugModels.get(i), state.lightCoords, poseStack, collector);
        }
        for (int i = 0; i < state.items.size(); i++) {
            Vec3 pos = state.itemPositions.get(i);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y - 0.1, pos.z);
            poseStack.scale(0.6f, 0.6f, 0.6f);
            state.items.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.fluidSprite != null) {
            submitFluid(state, poseStack, collector);
        }
        if (state.centerPower > 0.01f) {
            submitPower(state, poseStack, collector);
        }
    }

    private void submitFluid(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = state.fluidSprite;
        int colour = state.fluidColour;
        int light = state.lightCoords;
        double capacity = Math.max(1, state.fluidCapacity);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            // Centre: a cube that fills up from the bottom
            double center = Math.min(1, state.fluidAmounts[EnumPipePart.CENTER.getIndex()] / capacity);
            if (center > 0.001) {
                float h = (float) (7 * center);
                Face face = Face.full(sprite);
                BoxRenderer.box(pose, buf, 4.5f, 4.5f, 4.5f, 11.5f, 4.5f + h, 11.5f, faces(face), light, colour);
            }
            for (Direction dir : Direction.values()) {
                if (!state.connected[dir.get3DDataValue()]) continue;
                double amount = Math.min(1, state.fluidAmounts[dir.get3DDataValue()] / capacity);
                if (amount < 0.001) continue;
                Face face = Face.full(sprite);
                float h = (float) (7 * amount);
                switch (dir) {
                    case DOWN -> {
                        float r = (float) (3.5 * Math.sqrt(amount));
                        BoxRenderer.box(pose, buf, 8 - r, 0, 8 - r, 8 + r, 4.5f, 8 + r, faces(face), light, colour);
                    }
                    case UP -> {
                        float r = (float) (3.5 * Math.sqrt(amount));
                        BoxRenderer.box(pose, buf, 8 - r, 11.5f, 8 - r, 8 + r, 16, 8 + r, faces(face), light, colour);
                    }
                    case NORTH -> BoxRenderer.box(pose, buf, 4.5f, 4.5f, 0, 11.5f, 4.5f + h, 4.5f, faces(face), light, colour);
                    case SOUTH -> BoxRenderer.box(pose, buf, 4.5f, 4.5f, 11.5f, 11.5f, 4.5f + h, 16, faces(face), light, colour);
                    case WEST -> BoxRenderer.box(pose, buf, 0, 4.5f, 4.5f, 4.5f, 4.5f + h, 11.5f, faces(face), light, colour);
                    case EAST -> BoxRenderer.box(pose, buf, 11.5f, 4.5f, 4.5f, 16, 4.5f + h, 11.5f, faces(face), light, colour);
                }
            }
        });
    }

    private void submitPower(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = sprites.get(POWER_FLOW);
        int light = 0xF000F0;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            float c = 0.5f + 2.5f * state.centerPower;
            Face face = Face.full(sprite);
            BoxRenderer.box(pose, buf, 8 - c, 8 - c, 8 - c, 8 + c, 8 + c, 8 + c, faces(face), light, -1);
            for (Direction dir : Direction.values()) {
                int i = dir.get3DDataValue();
                if (!state.connected[i] || state.power[i] < 0.01f) continue;
                float r = 0.5f + 2.5f * state.power[i];
                float lo = 8 - r, hi = 8 + r;
                switch (dir) {
                    case DOWN -> BoxRenderer.box(pose, buf, lo, 0, lo, hi, 8 - c, hi, faces(face), light, -1);
                    case UP -> BoxRenderer.box(pose, buf, lo, 8 + c, lo, hi, 16, hi, faces(face), light, -1);
                    case NORTH -> BoxRenderer.box(pose, buf, lo, lo, 0, hi, hi, 8 - c, faces(face), light, -1);
                    case SOUTH -> BoxRenderer.box(pose, buf, lo, lo, 8 + c, hi, hi, 16, faces(face), light, -1);
                    case WEST -> BoxRenderer.box(pose, buf, 0, lo, lo, 8 - c, hi, hi, faces(face), light, -1);
                    case EAST -> BoxRenderer.box(pose, buf, 8 + c, lo, lo, 16, hi, hi, faces(face), light, -1);
                }
            }
        });
    }

    private void submitPluggable(Direction side, List<PlugModelPart> model, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        switch (side) {
            case EAST -> poseStack.rotate(Axis.YP.rotationDegrees(180));
            case NORTH -> poseStack.rotate(Axis.YP.rotationDegrees(-90));
            case SOUTH -> poseStack.rotate(Axis.YP.rotationDegrees(90));
            case UP -> poseStack.rotate(Axis.ZP.rotationDegrees(-90));
            case DOWN -> poseStack.rotate(Axis.ZP.rotationDegrees(90));
            default -> {}
        }
        poseStack.translate(-0.5, -0.5, -0.5);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
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
        poseStack.popPose();
    }

    private static Face[] faces(Face face) {
        return new Face[] { face, face, face, face, face, face };
    }
}
