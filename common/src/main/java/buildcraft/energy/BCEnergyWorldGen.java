package buildcraft.energy;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import buildcraft.BuildCraft;
import buildcraft.energy.worldgen.OilWellFeature;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCEnergyWorldGen {
    public static RegistryEntry<MapCodec<? extends Feature>, MapCodec<OilWellFeature>> OIL_WELL;
    /** The placed feature (in the data pack) that each loader adds to overworld biomes. */
    public static final ResourceKey<PlacedFeature> OIL_WELL_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, BuildCraft.id("oil_well"));

    private BCEnergyWorldGen() {}

    static void init() {
        OIL_WELL = BCRegistry.register(Registries.FEATURE_TYPE, "oil_well", key -> OilWellFeature.CODEC);
    }
}
