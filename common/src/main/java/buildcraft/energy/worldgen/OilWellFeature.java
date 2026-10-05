/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.resources.Identifier;

import buildcraft.api.enums.EnumSpring;
import buildcraft.core.worldgen.WaterSpringFeature;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.BCEnergyFluids;

/** Oil wells: an underground sphere of oil with a spout up to the surface, and a pool of oil on the surface. Lakes of oil
 * appear in deserts and oceans.
 * <p>
 * Each deposit is decided by a random number seeded from the chunk it is centred in, so every chunk that a deposit
 * reaches into can place its own part of it. */
public record OilWellFeature() implements Feature {
    public static final MapCodec<OilWellFeature> CODEC = MapCodec.unit(OilWellFeature::new);

    private static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;
    /** How far (in chunks) a deposit can reach from the chunk it is centred in. */
    private static final int MAX_CHUNK_RADIUS = 5;
    private static final TagKey<Biome> IS_DESERT = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_desert"));

    private enum GenType {
        LARGE,
        MEDIUM,
        LAKE
    }

    @Override
    public MapCodec<OilWellFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!BCEnergyConfig.enableOilGeneration) return false;
        ChunkPos chunk = ChunkPos.containing(origin);
        BoundingBox box = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
            level.getMaxY(), chunk.getMaxBlockZ());
        boolean placed = false;
        for (int cdx = -MAX_CHUNK_RADIUS; cdx <= MAX_CHUNK_RADIUS; cdx++) {
            for (int cdz = -MAX_CHUNK_RADIUS; cdz <= MAX_CHUNK_RADIUS; cdz++) {
                for (Structure structure : getStructures(level, generator, chunk.x() + cdx, chunk.z() + cdz)) {
                    placed |= structure.generate(level, box);
                }
            }
        }
        return placed;
    }

    private static List<Structure> getStructures(WorldGenLevel level, ChunkGenerator generator, int cx, int cz) {
        RandomSource rand = RandomSource.create(level.getSeed() ^ MAGIC_GEN_NUMBER ^ (cx * 341873128712L + cz * 132897987541L));
        int x = cx * 16 + rand.nextInt(16);
        int z = cz * 16 + rand.nextInt(16);

        double rollLarge = rand.nextDouble(), rollMedium = rand.nextDouble(), rollLake = rand.nextDouble();
        double rate = BCEnergyConfig.oilWellGenerationRate;
        // Oil biomes give at most 3 times the chance, so most chunks can stop here without looking at the biome
        if (rollLarge > BCEnergyConfig.largeOilGenProb * 3 * rate && rollMedium > BCEnergyConfig.mediumOilGenProb * 3 * rate
            && rollLake > BCEnergyConfig.smallOilGenProb * 3 * rate) {
            return List.of();
        }

        // Ask the biome source directly, as the chunk may not have been generated yet
        Holder<Biome> biome = generator.getBiomeSource().createUncachedResolver(level.getLevel().getChunkSource().randomState())
            .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z));
        if (biome.is(BiomeTags.IS_END) || biome.is(BiomeTags.IS_NETHER)) {
            return List.of();
        }
        boolean oilBiome = biome.is(IS_DESERT) || biome.is(BiomeTags.IS_OCEAN);
        double bonus = (oilBiome ? 3.0 : 1.0) * rate;

        GenType type;
        if (rollLarge <= BCEnergyConfig.largeOilGenProb * bonus) {
            type = GenType.LARGE;
        } else if (rollMedium <= BCEnergyConfig.mediumOilGenProb * bonus) {
            type = GenType.MEDIUM;
        } else if (oilBiome && rollLake <= BCEnergyConfig.smallOilGenProb * bonus) {
            type = GenType.LAKE;
        } else {
            return List.of();
        }

        List<Structure> structures = new ArrayList<>();
        int lakeRadius;
        int tendrilRadius;
        if (type == GenType.LARGE) {
            lakeRadius = 4;
            tendrilRadius = 25 + rand.nextInt(20);
        } else if (type == GenType.LAKE) {
            lakeRadius = 6;
            tendrilRadius = 25 + rand.nextInt(20);
        } else {
            lakeRadius = 2;
            tendrilRadius = 5 + rand.nextInt(10);
        }
        structures.add(createTendril(x, z, lakeRadius, tendrilRadius, rand));

        if (type != GenType.LAKE) {
            int wellY = 20 + rand.nextInt(10);
            int radius = type == GenType.LARGE ? 8 + rand.nextInt(9) : 4 + rand.nextInt(4);
            structures.add(sphere(new BlockPos(x, wellY, z), radius));

            if (BCEnergyConfig.enableOilSpouts) {
                int minHeight = type == GenType.LARGE ? BCEnergyConfig.largeSpoutMinHeight : BCEnergyConfig.smallSpoutMinHeight;
                int maxHeight = type == GenType.LARGE ? BCEnergyConfig.largeSpoutMaxHeight : BCEnergyConfig.smallSpoutMaxHeight;
                int height = maxHeight <= minHeight ? minHeight : minHeight + rand.nextInt(maxHeight - minHeight);
                structures.add(new Spout(new BlockPos(x, wellY, z), height, type == GenType.LARGE ? 1 : 0));
            }

            if (type == GenType.LARGE) {
                // A thin column of oil down to the bottom of the world
                structures.add(tubeY(new BlockPos(x, level.getMinY() + 1, z), wellY - level.getMinY() - 1, 1));
                // With an oil spring at the bottom, which slowly refills it
                structures.add(new OilSpring(new BlockPos(x, level.getMinY(), z)));
            }
        }
        return structures;
    }

    private static Structure sphere(BlockPos center, int radius) {
        double radiusSq = radius * radius + 0.01;
        BoundingBox box = new BoundingBox(center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius);
        return new ByPredicate(box, p -> p.distSqr(center) <= radiusSq);
    }

    /** A vertical cylinder starting at base. */
    static Structure tubeY(BlockPos base, int length, int radius) {
        double radiusSq = radius * radius;
        BoundingBox box = new BoundingBox(base.getX() - radius, base.getY(), base.getZ() - radius, base.getX() + radius,
            base.getY() + length, base.getZ() + radius);
        return new ByPredicate(box, p -> {
            int dx = p.getX() - base.getX(), dz = p.getZ() - base.getZ();
            return dx * dx + dz * dz <= radiusSq;
        });
    }

    private static Structure createTendril(int cx, int cz, int lakeRadius, int radius, RandomSource rand) {
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];
        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }
        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);
            fillIfProba(rand, proba, x, z + w, pattern);
            fillIfProba(rand, proba, x, z - w, pattern);
            fillIfProba(rand, proba, x + w, z, pattern);
            fillIfProba(rand, proba, x - w, z, pattern);
            for (int i = 1; i <= w; i++) {
                fillIfProba(rand, proba, x + i, z + w, pattern);
                fillIfProba(rand, proba, x + i, z - w, pattern);
                fillIfProba(rand, proba, x + w, z + i, pattern);
                fillIfProba(rand, proba, x - w, z + i, pattern);
                fillIfProba(rand, proba, x - i, z + w, pattern);
                fillIfProba(rand, proba, x - i, z - w, pattern);
                fillIfProba(rand, proba, x + w, z - i, pattern);
                fillIfProba(rand, proba, x - w, z - i, pattern);
            }
        }
        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return new SurfacePattern(cx - radius, cz - radius, pattern, depth);
    }

    private static void fillIfProba(RandomSource rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        return x >= 0 && x < pattern.length && z >= 0 && z < pattern[x].length && pattern[x][z];
    }

    static BlockState oil() {
        return BCEnergyFluids.crudeOil[0].block.get().defaultBlockState();
    }

    static boolean setOil(WorldGenLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) return false;
        return level.setBlock(pos, oil(), Block.UPDATE_CLIENTS);
    }

    /** Part of an oil deposit. */
    abstract static class Structure {
        final BoundingBox box;

        Structure(BoundingBox box) {
            this.box = box;
        }

        /** Places the part of this structure that's within the given box. */
        final boolean generate(WorldGenLevel level, BoundingBox within) {
            if (!box.intersects(within)) return false;
            BoundingBox intersect = new BoundingBox(Math.max(box.minX(), within.minX()), Math.max(box.minY(), within.minY()),
                Math.max(box.minZ(), within.minZ()), Math.min(box.maxX(), within.maxX()), Math.min(box.maxY(), within.maxY()),
                Math.min(box.maxZ(), within.maxZ()));
            return generateWithin(level, intersect);
        }

        abstract boolean generateWithin(WorldGenLevel level, BoundingBox intersect);
    }

    static class ByPredicate extends Structure {
        private final Predicate<BlockPos> predicate;

        ByPredicate(BoundingBox box, Predicate<BlockPos> predicate) {
            super(box);
            this.predicate = predicate;
        }

        @Override
        boolean generateWithin(WorldGenLevel level, BoundingBox intersect) {
            boolean placed = false;
            for (BlockPos pos : BlockPos.betweenClosed(intersect.minX(), intersect.minY(), intersect.minZ(), intersect.maxX(),
                intersect.maxY(), intersect.maxZ())) {
                if (predicate.test(pos)) {
                    placed |= setOil(level, pos);
                }
            }
            return placed;
        }
    }

    /** A pattern of oil sitting in the top of the terrain, clearing out the blocks above it. */
    static class SurfacePattern extends Structure {
        private final boolean[][] pattern;
        private final int depth;

        SurfacePattern(int minX, int minZ, boolean[][] pattern, int depth) {
            super(new BoundingBox(minX, -4096, minZ, minX + pattern.length - 1, 4096, minZ + pattern[0].length - 1));
            this.pattern = pattern;
            this.depth = depth;
        }

        @Override
        boolean generateWithin(WorldGenLevel level, BoundingBox intersect) {
            boolean placed = false;
            for (int x = intersect.minX(); x <= intersect.maxX(); x++) {
                for (int z = intersect.minZ(); z <= intersect.maxZ(); z++) {
                    if (!pattern[x - box.minX()][z - box.minZ()]) continue;
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    BlockPos upper = new BlockPos(x, top, z);
                    for (int y = 1; y < 5; y++) {
                        level.setBlock(upper.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    for (int y = 0; y < depth; y++) {
                        placed |= setOil(level, upper.below(y));
                    }
                }
            }
            return placed;
        }
    }

    static class OilSpring extends Structure {
        private final BlockPos pos;

        OilSpring(BlockPos pos) {
            super(new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
            this.pos = pos;
        }

        @Override
        boolean generateWithin(WorldGenLevel level, BoundingBox intersect) {
            WaterSpringFeature.placeSpring(level, pos, EnumSpring.OIL);
            return true;
        }
    }

    /** A column of oil from the deposit up to (and above) the surface. */
    static class Spout extends Structure {
        private final BlockPos start;
        private final int height;
        private final int radius;

        Spout(BlockPos start, int height, int radius) {
            super(new BoundingBox(start.getX(), start.getY(), start.getZ(), start.getX(), start.getY(), start.getZ()));
            this.start = start;
            this.height = height;
            this.radius = radius;
        }

        @Override
        boolean generateWithin(WorldGenLevel level, BoundingBox intersect) {
            // Only the chunk with the centre of the spout generates it; the tubes are narrow enough to stay in it
            int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, start.getX(), start.getZ());
            boolean placed = tubeY(start, top - start.getY(), radius).generateWithin(level, clampY(level, top));
            BlockPos base = new BlockPos(start.getX(), top, start.getZ());
            for (int r = radius; r >= 0; r--) {
                Structure tube = tubeY(base, height, r);
                placed |= tube.generateWithin(level, tube.box);
                base = base.above(height);
            }
            return placed;
        }

        private BoundingBox clampY(WorldGenLevel level, int top) {
            return new BoundingBox(start.getX() - radius, start.getY(), start.getZ() - radius, start.getX() + radius, top,
                start.getZ() + radius);
        }
    }
}
