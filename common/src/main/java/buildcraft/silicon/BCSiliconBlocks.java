package buildcraft.silicon;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import buildcraft.lib.registry.RegistryEntry;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.block.BlockLaserTable;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileLaser;

import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCSiliconBlocks {
    public static RegistryEntry<Block, BlockLaser> LASER;
    public static RegistryEntry<Block, BlockLaserTable<TileAssemblyTable>> ASSEMBLY_TABLE;
    public static RegistryEntry<Block, BlockLaserTable<TileAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileLaser>> LASER_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileAssemblyTable>> ASSEMBLY_TABLE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE_TILE;

    private BCSiliconBlocks() {}

    private static Supplier<BlockBehaviour.Properties> machine() {
        return () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
            .requiresCorrectToolForDrops();
    }

    static void init() {
        LASER = blockWithItem("laser", props -> new BlockLaser(props, () -> LASER_TILE.get()), machine());
        ASSEMBLY_TABLE = blockWithItem("assembly_table", props -> new BlockLaserTable<>(props, () -> ASSEMBLY_TABLE_TILE.get()), machine());
        ADVANCED_CRAFTING_TABLE = blockWithItem("advanced_crafting_table",
            props -> new BlockLaserTable<>(props, () -> ADVANCED_CRAFTING_TABLE_TILE.get()), machine());

        LASER_TILE = tile("laser", TileLaser::new, () -> LASER.get());
        ASSEMBLY_TABLE_TILE = tile("assembly_table", TileAssemblyTable::new, () -> ASSEMBLY_TABLE.get());
        ADVANCED_CRAFTING_TABLE_TILE = tile("advanced_crafting_table", TileAdvancedCraftingTable::new,
            () -> ADVANCED_CRAFTING_TABLE.get());
    }
}
