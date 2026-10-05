package buildcraft.core;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import buildcraft.core.tile.TileEngineCreative;
import buildcraft.core.tile.TileEngineRedstone;
import buildcraft.lib.engine.BlockEngine;
import buildcraft.lib.registry.RegistryEntry;

import net.minecraft.world.level.block.Block;

import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCCoreBlocks {
    public static RegistryEntry<Block, BlockEngine<TileEngineRedstone>> ENGINE_REDSTONE;
    public static RegistryEntry<Block, BlockEngine<TileEngineCreative>> ENGINE_CREATIVE;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileEngineRedstone>> ENGINE_REDSTONE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileEngineCreative>> ENGINE_CREATIVE_TILE;

    private BCCoreBlocks() {}

    static void init() {
        ENGINE_REDSTONE = blockWithItem("engine_redstone", props -> new BlockEngine<>(props, () -> ENGINE_REDSTONE_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD));
        ENGINE_CREATIVE = blockWithItem("engine_creative", props -> new BlockEngine<>(props, () -> ENGINE_CREATIVE_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1, 3600000).sound(SoundType.METAL));

        ENGINE_REDSTONE_TILE = tile("engine_redstone", TileEngineRedstone::new, () -> ENGINE_REDSTONE.get());
        ENGINE_CREATIVE_TILE = tile("engine_creative", TileEngineCreative::new, () -> ENGINE_CREATIVE.get());
    }
}
