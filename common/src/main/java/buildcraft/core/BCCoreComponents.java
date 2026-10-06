/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import buildcraft.core.item.MapLocation;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCCoreComponents {
    /** The marker that the marker connector will connect the next one to. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<BlockPos>> MARKER_LINK;

    /** The fluid in a fragile fluid shard. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<BCFluidStack>> FLUID;

    /** The place a map location remembers. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<MapLocation>> MAP_LOCATION;

    private BCCoreComponents() {}

    static void init() {
        MARKER_LINK = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "marker_link",
            key -> DataComponentType.<BlockPos> builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());
        FLUID = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "fluid",
            key -> DataComponentType.<BCFluidStack> builder().persistent(BCFluidStack.CODEC).networkSynchronized(BCFluidStack.STREAM_CODEC)
                .build());
        MAP_LOCATION = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "map_location",
            key -> DataComponentType.<MapLocation> builder().persistent(MapLocation.CODEC).networkSynchronized(MapLocation.STREAM_CODEC)
                .build());
    }
}
