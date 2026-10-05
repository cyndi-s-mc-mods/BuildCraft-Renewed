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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import buildcraft.BuildCraft;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.EnumWirePart;
import buildcraft.transport.wire.WireManager;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.client.render.BoxRenderer;
import buildcraft.client.render.BoxRenderer.Face;
import buildcraft.client.render.PlugPartRenderer;
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
        /** Wire boxes: x0, y0, z0, x1, y1, z1 in pixels, then the colour ordinal. */
        final List<float[]> wireBoxes = new ArrayList<>();
        /** The pipe's paint colour (ARGB), or 0 if it isn't painted. */
        int paint;
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
        state.wireBoxes.clear();
        DyeColor paint = tile.getPipe().getColour();
        state.paint = paint == null ? 0 : 0xFF000000 | paint.getTextureDiffuseColor();
        Level wireLevel = tile.getLevel();
        if (wireLevel != null) {
            for (Map.Entry<EnumWirePart, DyeColor> wire : tile.getWires().entrySet()) {
                addWireBoxes(state.wireBoxes, wireLevel, tile, wire.getKey(), wire.getValue());
            }
        }
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
        if (state.paint != 0) {
            submitPaint(state, poseStack, collector);
        }
        if (!state.wireBoxes.isEmpty()) {
            submitWires(state.wireBoxes, state.lightCoords, poseStack, collector);
        }
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

    /** Adds the boxes for one wire: a small cube at its corner, and bars to the wires it connects to. */
    private static void addWireBoxes(List<float[]> boxes, Level level, TilePipeHolder tile, EnumWirePart part, DyeColor colour) {
        float[] lo = new float[3], hi = new float[3];
        boolean[] signs = { part.x, part.y, part.z };
        for (int a = 0; a < 3; a++) {
            lo[a] = signs[a] ? 12 : 3;
            hi[a] = signs[a] ? 13 : 4;
        }
        boxes.add(new float[] { lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], colour.ordinal() });
        for (Direction.Axis axis : Direction.Axis.values()) {
            int a = axis.ordinal();
            // Along the pipe to the wire in the next corner (drawn once, from the negative corner)
            if (!signs[a] && tile.getWires().get(part.flip(axis)) == colour) {
                float[] l = lo.clone(), h = hi.clone();
                l[a] = 4;
                h[a] = 12;
                boxes.add(new float[] { l[0], l[1], l[2], h[0], h[1], h[2], colour.ordinal() });
            }
            // Out to the wire in the next pipe
            Direction side = Direction.fromAxisAndDirection(axis, signs[a] ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            if (WireManager.connectsAcross(level, tile.getBlockPos(), part, colour, side)) {
                float[] l = lo.clone(), h = hi.clone();
                if (signs[a]) {
                    l[a] = 13;
                    h[a] = 16;
                } else {
                    l[a] = 0;
                    h[a] = 3;
                }
                boxes.add(new float[] { l[0], l[1], l[2], h[0], h[1], h[2], colour.ordinal() });
            }
        }
    }

    /** Draws a coloured border around a painted pipe's centre and arms. */
    private void submitPaint(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/pipes/colour_border_outer")));
        int light = state.lightCoords;
        int colour = state.paint;
        float lo = 3.98f, hi = 12.02f;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            Face[] centre = new Face[6];
            for (Direction dir : Direction.values()) {
                if (!state.connected[dir.get3DDataValue()]) centre[dir.get3DDataValue()] = new Face(sprite, 4, 4, 12, 12);
            }
            BoxRenderer.box(pose, buf, lo, lo, lo, hi, hi, hi, centre, light, colour);
            for (Direction dir : Direction.values()) {
                if (!state.connected[dir.get3DDataValue()]) continue;
                Face[] faces = new Face[6];
                for (Direction f : Direction.values()) {
                    if (f.getAxis() != dir.getAxis()) faces[f.get3DDataValue()] = new Face(sprite, 0, 4, 4, 12);
                }
                float[] min = { lo, lo, lo }, max = { hi, hi, hi };
                int a = dir.getAxis().ordinal();
                if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    min[a] = 12;
                    max[a] = 16;
                } else {
                    min[a] = 0;
                    max[a] = 4;
                }
                BoxRenderer.box(pose, buf, min[0], min[1], min[2], max[0], max[1], max[2], faces, light, colour);
            }
        });
    }

    private void submitWires(List<float[]> boxes, int light, PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
            for (float[] b : boxes) {
                DyeColor colour = DyeColor.byId((int) b[6]);
                String name = colour == DyeColor.LIGHT_GRAY ? "silver" : colour.getSerializedName();
                TextureAtlasSprite sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, BuildCraft.id("block/wires/" + name)));
                BoxRenderer.box(pose, buf, b[0], b[1], b[2], b[3], b[4], b[5], faces(Face.full(sprite)), light, -1);
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
        PlugPartRenderer.submit(model, sprites, light, poseStack, collector);
        poseStack.popPose();
    }

    private static Face[] faces(Face face) {
        return new Face[] { face, face, face, face, face, face };
    }
}
