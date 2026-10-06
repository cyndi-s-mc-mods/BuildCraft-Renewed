/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import buildcraft.builders.block.BlockFacing;
import buildcraft.lib.registry.RegistryEntry;
import buildcraft.robotics.tile.TileZonePlanner;

import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCRoboticsBlocks {
    public static RegistryEntry<Block, BlockFacing<TileZonePlanner>> ZONE_PLANNER;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileZonePlanner>> ZONE_PLANNER_TILE;

    private BCRoboticsBlocks() {}

    static void init() {
        ZONE_PLANNER = blockWithItem("zone_planner", props -> new BlockFacing<>(props, () -> ZONE_PLANNER_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL).requiresCorrectToolForDrops());
        ZONE_PLANNER_TILE = tile("zone_planner", TileZonePlanner::new, () -> ZONE_PLANNER.get());
    }
}
