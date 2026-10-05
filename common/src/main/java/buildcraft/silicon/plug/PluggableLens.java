/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.silicon.item.ItemPluggableLens;

/** A lens paints the items that pass through its side. A filter only lets items of its colour through (and sends them
 * that way first). */
public class PluggableLens extends PipePluggable {
    private static final Identifier FRAME = BuildCraft.id("block/plugs/lens");
    private static final Identifier OVERLAY = BuildCraft.id("block/plugs/overlay_lens");
    private static final Identifier FILTER = BuildCraft.id("block/plugs/filter");
    private static final Identifier WATER = Identifier.withDefaultNamespace("block/water_still");

    @Nullable
    public final DyeColor colour;
    public final boolean isFilter;

    public PluggableLens(PluggableDefinition definition, IPipeHolder holder, Direction side, @Nullable DyeColor colour, boolean isFilter) {
        super(definition, holder, side);
        this.colour = colour;
        this.isFilter = isFilter;
    }

    public static PluggableLens load(PluggableDefinition definition, IPipeHolder holder, Direction side, ValueInput input) {
        int c = input.getIntOr("colour", -1);
        return new PluggableLens(definition, holder, side, c < 0 ? null : DyeColor.byId(c), input.getBooleanOr("filter", false));
    }

    @Override
    public void save(ValueOutput output) {
        output.putInt("colour", colour == null ? -1 : colour.getId());
        output.putBoolean("filter", isFilter);
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(0, 3, 3, 2, 13, 13, side);
    }

    @Override
    public ItemStack getPickStack() {
        return ItemPluggableLens.getStack(colour, isFilter);
    }

    @PipeEventHandler
    public void tryInsert(PipeEventItem.TryInsert event) {
        if (isFilter && event.from == side && event.colour != null && event.colour != colour) {
            event.cancel();
        }
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck event) {
        if (!isFilter) return;
        if (event.colour == colour) {
            event.increasePriority(side, 1);
        } else if (event.colour != null) {
            event.disallow(side);
        } else {
            event.decreasePriority(side, 1);
        }
    }

    @PipeEventHandler
    public void onInsert(PipeEventItem.OnInsert event) {
        if (!isFilter && event.from == side) {
            event.colour = colour;
        }
    }

    @PipeEventHandler
    public void reachEnd(PipeEventItem.ReachEnd event) {
        if (!isFilter && event.to == side) {
            event.colour = colour;
        }
    }

    @Override
    public List<PlugModelPart> getModel() {
        List<PlugModelPart> parts = new ArrayList<>();
        // The frame: four bars around the edge
        parts.add(PlugModelPart.autoUv(0, 3, 3, 2, 4, 13, FRAME));
        parts.add(PlugModelPart.autoUv(0, 12, 3, 2, 13, 13, FRAME));
        parts.add(PlugModelPart.autoUv(0, 4, 3, 2, 12, 4, FRAME));
        parts.add(PlugModelPart.autoUv(0, 4, 12, 2, 12, 13, FRAME));
        if (isFilter) {
            parts.add(PlugModelPart.autoUv(0.75f, 4, 4, 1.25f, 12, 12, FILTER));
        }
        // The glass, tinted with the lens colour
        int tint = colour == null ? 0x80FFFFFF : (0xA0 << 24) | (colour.getTextureDiffuseColor() & 0xFFFFFF);
        PlugModelPart glass = PlugModelPart.autoUv(0.5f, 4, 4, 1.5f, 12, 12, colour == null ? WATER : OVERLAY);
        parts.add(new PlugModelPart(glass.x0(), glass.y0(), glass.z0(), glass.x1(), glass.y1(), glass.z1(), glass.faces(), tint));
        return parts;
    }
}
