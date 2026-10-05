package buildcraft.builders;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import buildcraft.builders.block.BlockFrame;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.block;
import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCBuildersBlocks {
    public static RegistryEntry<Block, BlockQuarry> QUARRY;
    public static RegistryEntry<Block, BlockFrame> FRAME;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileQuarry>> QUARRY_TILE;

    private BCBuildersBlocks() {}

    static void init() {
        QUARRY = blockWithItem("quarry", props -> new BlockQuarry(props, () -> QUARRY_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10f).sound(SoundType.ANVIL)
                .requiresCorrectToolForDrops());
        FRAME = block("frame", BlockFrame::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.METAL)
                .noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE));

        QUARRY_TILE = tile("quarry", TileQuarry::new, () -> QUARRY.get());
    }
}
