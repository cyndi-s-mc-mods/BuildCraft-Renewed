/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import buildcraft.BuildCraft;
import buildcraft.core.worldgen.WaterSpringFeature;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCCoreWorldGen {
    public static RegistryEntry<MapCodec<? extends Feature>, MapCodec<WaterSpringFeature>> WATER_SPRING;
    /** The placed feature (in the data pack) that each loader adds to overworld biomes. */
    public static final ResourceKey<PlacedFeature> WATER_SPRING_PLACED =
        ResourceKey.create(Registries.PLACED_FEATURE, BuildCraft.id("water_spring"));

    private BCCoreWorldGen() {}

    static void init() {
        WATER_SPRING = BCRegistry.register(Registries.FEATURE_TYPE, "water_spring", key -> WaterSpringFeature.CODEC);
    }
}
