/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Works out what the zone planner's map shows for a chunk: the map colour of the top block of each column, shaded by
 * how its height compares to the column north of it (as vanilla maps do). */
public final class ZoneMapColours {
    private ZoneMapColours() {}

    public static int[] compute(ServerLevel level, int chunkX, int chunkZ) {
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        int[] colours = new int[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int worldX = (chunkX << 4) + x, worldZ = (chunkZ << 4) + z;
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set(worldX, y, worldZ);
                BlockState state = chunk.getBlockState(pos);
                MapColor colour = state.getMapColor(level, pos);
                int northY = z > 0 ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) : y;
                MapColor.Brightness brightness = y > northY ? MapColor.Brightness.HIGH
                    : y < northY ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                colours[x + z * 16] = colour == MapColor.NONE ? 0xFF000000 : colour.calculateARGBColor(brightness);
            }
        }
        return colours;
    }
}
