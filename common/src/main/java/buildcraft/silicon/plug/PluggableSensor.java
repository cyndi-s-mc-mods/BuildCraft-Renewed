/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PlugModelPart;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.lib.misc.RotationUtil;

/** A small plate that gives gates extra triggers: the light sensor and the timer. */
public class PluggableSensor extends PipePluggable {
    private final Identifier texture;
    private final Supplier<? extends Item> item;

    public PluggableSensor(PluggableDefinition definition, IPipeHolder holder, Direction side, Identifier texture,
        Supplier<? extends Item> item) {
        super(definition, holder, side);
        this.texture = texture;
        this.item = item;
    }

    @Override
    public AABB getBoundingBox() {
        return RotationUtil.boxFromWest(2, 5, 5, 4, 11, 11, side);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(item.get());
    }

    @Override
    public List<PlugModelPart> getModel() {
        return List.of(SimplePlugModels.plate(texture, 3));
    }
}
