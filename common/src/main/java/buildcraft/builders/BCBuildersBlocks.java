package buildcraft.builders;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.block.BlockFacing;
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.block.BlockFrame;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.block;
import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCBuildersBlocks {
    public static RegistryEntry<Block, BlockQuarry> QUARRY;
    public static RegistryEntry<Block, BlockFrame> FRAME;
    public static RegistryEntry<Block, BlockFiller> FILLER;
    public static RegistryEntry<Block, BlockArchitectTable> ARCHITECT;
    public static RegistryEntry<Block, BlockFacing<TileBuilder>> BUILDER;
    public static RegistryEntry<Block, BlockFacing<TileElectronicLibrary>> LIBRARY;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileQuarry>> QUARRY_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileFiller>> FILLER_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileArchitectTable>> ARCHITECT_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileBuilder>> BUILDER_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileElectronicLibrary>> LIBRARY_TILE;

    private BCBuildersBlocks() {}

    static void init() {
        QUARRY = blockWithItem("quarry", props -> new BlockQuarry(props, () -> QUARRY_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10f).sound(SoundType.ANVIL)
                .requiresCorrectToolForDrops());
        FRAME = block("frame", BlockFrame::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.METAL)
                .noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE));

        FILLER = blockWithItem("filler", props -> new BlockFiller(props, () -> FILLER_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        ARCHITECT = blockWithItem("architect", props -> new BlockArchitectTable(props, () -> ARCHITECT_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        BUILDER = blockWithItem("builder", props -> new BlockFacing<>(props, () -> BUILDER_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        LIBRARY = blockWithItem("library", props -> new BlockFacing<>(props, () -> LIBRARY_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
                .requiresCorrectToolForDrops());

        QUARRY_TILE = tile("quarry", TileQuarry::new, () -> QUARRY.get());
        FILLER_TILE = tile("filler", TileFiller::new, () -> FILLER.get());
        ARCHITECT_TILE = tile("architect", TileArchitectTable::new, () -> ARCHITECT.get());
        BUILDER_TILE = tile("builder", TileBuilder::new, () -> BUILDER.get());
        LIBRARY_TILE = tile("library", TileElectronicLibrary::new, () -> LIBRARY.get());
    }
}
