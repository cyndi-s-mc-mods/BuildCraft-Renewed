/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.silicon.item.ItemPluggableFacade;

/** Covers a side of a pipe with a block. A solid facade stops the pipe connecting on that side; a hollow one has a hole
 * in the middle that the pipe can go through. */
public class PluggableFacade extends PipePluggable {
    /** The texture and tint of one face of a block. */
    public record FaceTexture(Identifier sprite, int tint) {}

    /** Looks up the texture of a block's face. Set by the client, as only it has the block models. */
    public static BiFunction<FacadeData, Direction, FaceTexture> textureLookup = (data, dir) ->
        new FaceTexture(Identifier.withDefaultNamespace("block/stone"), -1);

    private static final float[][] SOLID = { { 0, 0, 0, 2, 16, 16 } };
    private static final float[][] HOLLOW = {
        { 0, 0, 0, 2, 4, 16 }, { 0, 12, 0, 2, 16, 16 }, { 0, 4, 0, 2, 12, 4 }, { 0, 4, 12, 2, 12, 16 } };

    public final FacadeData data;
    private @Nullable List<PlugModelPart> model;

    public PluggableFacade(PluggableDefinition definition, IPipeHolder holder, Direction side, FacadeData data) {
        super(definition, holder, side);
        this.data = data;
    }

    public static PluggableFacade load(PluggableDefinition definition, IPipeHolder holder, Direction side, ValueInput input) {
        FacadeData data = input.read("facade", FacadeData.CODEC).orElse(new FacadeData(Blocks.STONE.defaultBlockState(), false));
        return new PluggableFacade(definition, holder, side, data);
    }

    @Override
    public void save(ValueOutput output) {
        output.store("facade", FacadeData.CODEC, data);
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(0, 0, 0, 2, 16, 16, side);
    }

    @Override
    public boolean isBlocking() {
        return !data.hollow();
    }

    @Override
    public ItemStack getPickStack() {
        return ItemPluggableFacade.getStack(data, 1);
    }

    @Override
    public List<PlugModelPart> getModel() {
        if (model == null) {
            model = buildModel(data, side);
        }
        return model;
    }

    /** @return The facade's boxes, as they would be on the west side, with the textures of the faces they'll have once
     *         turned to the given side. Each face is its own part, so that it can have its own tint. */
    public static List<PlugModelPart> buildModel(FacadeData data, Direction side) {
        List<PlugModelPart> parts = new ArrayList<>();
        for (float[] b : data.hollow() ? HOLLOW : SOLID) {
            for (Direction local : Direction.values()) {
                FaceTexture tex = textureLookup.apply(data, RotationUtil.directionFromWest(local, side));
                PlugModelPart.Face[] all = PlugModelPart.autoUv(b[0], b[1], b[2], b[3], b[4], b[5], tex.sprite()).faces();
                PlugModelPart.Face[] faces = new PlugModelPart.Face[6];
                faces[local.get3DDataValue()] = all[local.get3DDataValue()];
                parts.add(new PlugModelPart(b[0], b[1], b[2], b[3], b[4], b[5], faces, tex.tint() | 0xFF000000));
            }
        }
        return parts;
    }
}
