/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCBuildersComponents {
    public static RegistryEntry<DataComponentType<?>, DataComponentType<SnapshotHeader>> SNAPSHOT;

    private BCBuildersComponents() {}

    static void init() {
        SNAPSHOT = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "snapshot",
            key -> DataComponentType.<SnapshotHeader> builder().persistent(SnapshotHeader.CODEC)
                .networkSynchronized(SnapshotHeader.STREAM_CODEC).build());
    }
}
