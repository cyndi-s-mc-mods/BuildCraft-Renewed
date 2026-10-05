/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.plug;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import buildcraft.BuildCraft;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.transport.BCTransportItems;

/** Lets engines power pipes that take power directly (like wooden pipes), without the pipe connecting to them. */
public class PluggablePowerAdaptor extends PipePluggable {
    private static final Identifier TEXTURE = BuildCraft.id("block/pipes/power_adapter");
    private static final List<PlugModelPart> MODEL = List.of(
        PlugModelPart.box(2, 4, 4, 4.01f, 12, 12, new PlugModelPart.Face[] {
            new PlugModelPart.Face(TEXTURE, 2, 4, 4, 12), new PlugModelPart.Face(TEXTURE, 2, 4, 4, 12),
            new PlugModelPart.Face(TEXTURE, 2, 4, 4, 12), new PlugModelPart.Face(TEXTURE, 2, 4, 4, 12),
            new PlugModelPart.Face(TEXTURE, 4, 4, 12, 12), new PlugModelPart.Face(TEXTURE, 4, 4, 12, 12) }),
        PlugModelPart.box(0, 3, 3, 2, 13, 13, new PlugModelPart.Face[] {
            new PlugModelPart.Face(TEXTURE, 0, 3, 2, 13), new PlugModelPart.Face(TEXTURE, 0, 3, 2, 13),
            new PlugModelPart.Face(TEXTURE, 0, 3, 2, 13), new PlugModelPart.Face(TEXTURE, 0, 3, 2, 13),
            new PlugModelPart.Face(TEXTURE, 3, 3, 13, 13), new PlugModelPart.Face(TEXTURE, 3, 3, 13, 13) }));

    public PluggablePowerAdaptor(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(0, 3, 3, 4, 13, 13, side);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @Nullable IMjConnector getMjConnector() {
        return holder.getPipe().getBehaviour().getMjConnector(side);
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCTransportItems.PLUG_POWER_ADAPTOR.get());
    }

    @Override
    public List<PlugModelPart> getModel() {
        return MODEL;
    }
}
