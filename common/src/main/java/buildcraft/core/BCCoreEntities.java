/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import buildcraft.core.marker.VolumeBoxEntity;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCCoreEntities {
    public static RegistryEntry<EntityType<?>, EntityType<VolumeBoxEntity>> VOLUME_BOX;

    private BCCoreEntities() {}

    static void init() {
        VOLUME_BOX = BCRegistry.register(Registries.ENTITY_TYPE, "volume_box",
            key -> EntityType.Builder.<VolumeBoxEntity> of(VolumeBoxEntity::new, MobCategory.MISC).sized(1, 1).clientTrackingRange(16)
                .updateInterval(10).fireImmune().build(key));
    }
}
