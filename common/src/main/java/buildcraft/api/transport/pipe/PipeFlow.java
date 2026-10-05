/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.IMjConnector;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;

/** What a pipe carries: items, fluids, power or nothing (structure pipes). */
public abstract class PipeFlow {
    public final IPipe pipe;

    public PipeFlow(IPipe pipe) {
        this.pipe = pipe;
    }

    public void save(ValueOutput output) {}

    public void load(ValueInput input) {}

    public abstract boolean canConnect(Direction face, PipeFlow other);

    public abstract boolean canConnect(Direction face, BlockEntity oTile);

    public boolean shouldForceConnection(Direction face, BlockEntity oTile) {
        return false;
    }

    public void onTick() {}

    public void addDrops(List<ItemStack> toDrop) {}

    @Nullable
    public IItemTransactor getItemTransactor(@Nullable Direction side) {
        return null;
    }

    @Nullable
    public IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return null;
    }

    @Nullable
    public IMjConnector getMjConnector(Direction side) {
        return null;
    }
}
