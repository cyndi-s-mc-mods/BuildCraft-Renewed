/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.transport.container.ContainerDiamondPipe;

/** Diamond pipes sort by filters: 9 per side. */
public abstract class PipeBehaviourDiamond extends PipeBehaviour {
    public static final int FILTERS_PER_SIDE = 9;

    public final ItemHandlerSimple filters = new ItemHandlerSimple(FILTERS_PER_SIDE * 6, this::onFiltersChanged);

    public PipeBehaviourDiamond(IPipe pipe) {
        super(pipe);
    }

    private void onFiltersChanged() {
        if (pipe.getHolder().getPipeWorld() != null && !pipe.getHolder().getPipeWorld().isClientSide()) {
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        filters.save(output, "filters");
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        filters.load(input, "filters");
    }

    @Override
    public @Nullable MenuProvider getMenuProvider() {
        return new SimpleMenuProvider((id, inv, player) -> new ContainerDiamondPipe(id, inv, this),
            Component.translatable(((net.minecraft.world.level.block.entity.BlockEntity) pipe.getHolder()).getBlockState()
                .getBlock().getDescriptionId()));
    }
}
