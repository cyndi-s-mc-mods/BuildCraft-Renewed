package buildcraft.core;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.EnumMap;
import java.util.Map;

import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.core.block.BlockMarker;
import buildcraft.core.block.BlockMarkerVolume;
import buildcraft.core.block.BlockSpring;
import buildcraft.core.tile.TilePowerTester;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.core.tile.TileEngineCreative;
import buildcraft.core.tile.TileMarkerPath;
import buildcraft.core.tile.TileMarkerVolume;
import buildcraft.core.tile.TileEngineRedstone;
import buildcraft.lib.engine.BlockEngine;
import buildcraft.lib.registry.RegistryEntry;

import net.minecraft.world.level.block.Block;

import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCCoreBlocks {
    public static RegistryEntry<Block, BlockEngine<TileEngineRedstone>> ENGINE_REDSTONE;
    public static RegistryEntry<Block, BlockEngine<TileEngineCreative>> ENGINE_CREATIVE;

    public static RegistryEntry<Block, BlockMarkerVolume> MARKER_VOLUME;
    public static RegistryEntry<Block, BlockMarker<TileMarkerPath>> MARKER_PATH;
    public static RegistryEntry<Block, BlockSpring> SPRING;
    public static RegistryEntry<Block, BlockBCTile<TilePowerTester>> POWER_TESTER;
    public static final Map<EnumDecoratedBlock, RegistryEntry<Block, Block>> DECORATED = new EnumMap<>(EnumDecoratedBlock.class);

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileEngineRedstone>> ENGINE_REDSTONE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileMarkerVolume>> MARKER_VOLUME_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileMarkerPath>> MARKER_PATH_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileEngineCreative>> ENGINE_CREATIVE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TilePowerTester>> POWER_TESTER_TILE;

    private BCCoreBlocks() {}

    static void init() {
        ENGINE_REDSTONE = blockWithItem("engine_redstone", props -> new BlockEngine<>(props, () -> ENGINE_REDSTONE_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD));
        ENGINE_CREATIVE = blockWithItem("engine_creative", props -> new BlockEngine<>(props, () -> ENGINE_CREATIVE_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1, 3600000).sound(SoundType.METAL));

        MARKER_VOLUME = blockWithItem("marker_volume", props -> new BlockMarkerVolume(props, () -> MARKER_VOLUME_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).instabreak().lightLevel(state -> 7).sound(SoundType.WOOD)
                .pushReaction(PushReaction.POPPED));
        MARKER_PATH = blockWithItem("marker_path", props -> new BlockMarker<>(props, () -> MARKER_PATH_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).instabreak().lightLevel(state -> 7).sound(SoundType.WOOD)
                .pushReaction(PushReaction.POPPED));

        SPRING = blockWithItem("spring", BlockSpring::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1, 3600000).noLootTable().sound(SoundType.STONE)
                .isValidSpawn((state, level, pos, type) -> false));
        POWER_TESTER = blockWithItem("power_tester", props -> new BlockBCTile<>(props, () -> POWER_TESTER_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5).sound(SoundType.METAL));
        for (EnumDecoratedBlock type : EnumDecoratedBlock.values()) {
            DECORATED.put(type, blockWithItem("decorated_" + type.getSerializedName(), Block::new,
                () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5).sound(SoundType.METAL)
                    .lightLevel(state -> type.lightValue)));
        }

        POWER_TESTER_TILE = tile("power_tester", TilePowerTester::new, () -> POWER_TESTER.get());
        MARKER_VOLUME_TILE = tile("marker_volume", TileMarkerVolume::new, () -> MARKER_VOLUME.get());
        MARKER_PATH_TILE = tile("marker_path", TileMarkerPath::new, () -> MARKER_PATH.get());
        ENGINE_REDSTONE_TILE = tile("engine_redstone", TileEngineRedstone::new, () -> ENGINE_REDSTONE.get());
        ENGINE_CREATIVE_TILE = tile("engine_creative", TileEngineCreative::new, () -> ENGINE_CREATIVE.get());
    }
}
