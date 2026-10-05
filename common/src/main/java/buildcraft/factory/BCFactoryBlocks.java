package buildcraft.factory;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.block.BlockPump;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileFloodGate;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TilePump;
import buildcraft.factory.tile.TileTank;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.block;
import static buildcraft.lib.registry.RegistrationHelper.blockWithItem;
import static buildcraft.lib.registry.RegistrationHelper.tile;

public final class BCFactoryBlocks {
    public static RegistryEntry<Block, BlockTank> TANK;
    public static RegistryEntry<Block, BlockMiningWell> MINING_WELL;
    public static RegistryEntry<Block, BlockPump> PUMP;
    public static RegistryEntry<Block, BlockTube> TUBE;
    public static RegistryEntry<Block, BlockChute> CHUTE;
    public static RegistryEntry<Block, BlockFloodGate> FLOOD_GATE;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileTank>> TANK_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileMiningWell>> MINING_WELL_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TilePump>> PUMP_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileChute>> CHUTE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileFloodGate>> FLOOD_GATE_TILE;

    private BCFactoryBlocks() {}

    private static Supplier<BlockBehaviour.Properties> machine() {
        return () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
            .requiresCorrectToolForDrops();
    }

    static void init() {
        TANK = blockWithItem("tank", props -> new BlockTank(props, () -> TANK_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS)
                .noOcclusion());
        MINING_WELL = blockWithItem("mining_well", props -> new BlockMiningWell(props, () -> MINING_WELL_TILE.get()), machine());
        PUMP = blockWithItem("pump", props -> new BlockPump(props, () -> PUMP_TILE.get()), machine());
        CHUTE = blockWithItem("chute", props -> new BlockChute(props, () -> CHUTE_TILE.get()), machine());
        FLOOD_GATE = blockWithItem("flood_gate", props -> new BlockFloodGate(props, () -> FLOOD_GATE_TILE.get()), machine());
        TUBE = block("tube", BlockTube::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1, 3_600_000).sound(SoundType.METAL)
                .noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE));

        TANK_TILE = tile("tank", TileTank::new, () -> TANK.get());
        MINING_WELL_TILE = tile("mining_well", TileMiningWell::new, () -> MINING_WELL.get());
        PUMP_TILE = tile("pump", TilePump::new, () -> PUMP.get());
        CHUTE_TILE = tile("chute", TileChute::new, () -> CHUTE.get());
        FLOOD_GATE_TILE = tile("flood_gate", TileFloodGate::new, () -> FLOOD_GATE.get());
    }
}
