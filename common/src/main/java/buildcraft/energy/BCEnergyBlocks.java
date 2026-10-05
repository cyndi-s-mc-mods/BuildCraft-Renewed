package buildcraft.energy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import buildcraft.energy.tile.TileEngineStone;
import buildcraft.lib.engine.BlockEngine;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCEnergyBlocks {
    public static RegistryEntry<Block, BlockEngine<TileEngineStone>> ENGINE_STIRLING;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileEngineStone>> ENGINE_STIRLING_TILE;

    private BCEnergyBlocks() {}

    static void init() {
        ENGINE_STIRLING = blockWithItem("engine_stirling", props -> new BlockEngine<>(props, () -> ENGINE_STIRLING_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f).sound(SoundType.STONE)
                .requiresCorrectToolForDrops());

        ENGINE_STIRLING_TILE = tile("engine_stirling", TileEngineStone::new, () -> ENGINE_STIRLING.get());
    }
}
