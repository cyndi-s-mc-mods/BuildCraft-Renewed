/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import buildcraft.client.render.PlugPartRenderer;
import buildcraft.silicon.BCSiliconComponents;
import buildcraft.silicon.plug.FacadeData;
import buildcraft.silicon.plug.PluggableFacade;
import buildcraft.silicon.plug.PluggableFacade.FaceTexture;

/** Gives facades the textures of the blocks they copy, and draws facade items. */
public final class FacadeClient {
    private FacadeClient() {}

    public static void init() {
        PluggableFacade.textureLookup = FacadeClient::getTexture;
    }

    private static FaceTexture getTexture(FacadeData data, Direction dir) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(data.state());
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42), parts);
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(dir)) {
                return texture(data, quad);
            }
        }
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == dir) return texture(data, quad);
            }
        }
        return new FaceTexture(model.particleMaterial().sprite().contents().name(), -1);
    }

    private static FaceTexture texture(FacadeData data, BakedQuad quad) {
        int tint = -1;
        if (quad.materialInfo().isTinted()) {
            BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(data.state(), quad.materialInfo().tintIndex());
            if (source != null) tint = source.color(data.state());
        }
        return new FaceTexture(quad.materialInfo().sprite().contents().name(), tint);
    }

    /** Draws a facade item as a thin plate of its block. */
    public static final class ItemRenderer implements SpecialModelRenderer<FacadeData> {
        private final SpriteGetter sprites;

        public ItemRenderer(SpriteGetter sprites) {
            this.sprites = sprites;
        }

        @Override
        public void submit(@Nullable FacadeData data, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
            boolean hasFoil, int outlineColor) {
            if (data == null) data = new FacadeData(Blocks.STONE.defaultBlockState(), false);
            poseStack.pushPose();
            poseStack.translate(7 / 16f, 0, 0);
            PlugPartRenderer.submit(PluggableFacade.buildModel(data, Direction.WEST), sprites, light, poseStack, collector);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            for (int i = 0; i < 8; i++) {
                output.accept(new Vector3f((i & 1) == 0 ? 7 / 16f : 9 / 16f, (i & 2) == 0 ? 0 : 1, (i & 4) == 0 ? 0 : 1));
            }
        }

        @Override
        public @Nullable FacadeData extractArgument(ItemStack stack) {
            return stack.get(BCSiliconComponents.FACADE.get());
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<FacadeData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<FacadeData> bake(SpecialModelRenderer.BakingContext context) {
            return new ItemRenderer(context.sprites());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
