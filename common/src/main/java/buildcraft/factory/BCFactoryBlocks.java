package buildcraft.factory;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import buildcraft.factory.block.BlockAutoWorkbench;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.block.BlockPump;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.tile.TileAutoWorkbench;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller;
import buildcraft.factory.tile.TileHeatExchange;
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
    public static RegistryEntry<Block, BlockDistiller> DISTILLER;
    public static RegistryEntry<Block, BlockHeatExchange> HEAT_EXCHANGE;
    public static RegistryEntry<Block, BlockAutoWorkbench> AUTO_WORKBENCH;
    public static RegistryEntry<Block, BlockWaterGel> WATER_GEL;

    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileTank>> TANK_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileMiningWell>> MINING_WELL_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TilePump>> PUMP_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileChute>> CHUTE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileFloodGate>> FLOOD_GATE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileDistiller>> DISTILLER_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileHeatExchange>> HEAT_EXCHANGE_TILE;
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TileAutoWorkbench>> AUTO_WORKBENCH_TILE;

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
        DISTILLER = blockWithItem("distiller", props -> new BlockDistiller(props, () -> DISTILLER_TILE.get()), machine());
        HEAT_EXCHANGE = blockWithItem("heat_exchange", props -> new BlockHeatExchange(props, () -> HEAT_EXCHANGE_TILE.get()), machine());
        AUTO_WORKBENCH = blockWithItem("autoworkbench_item", props -> new BlockAutoWorkbench(props, () -> AUTO_WORKBENCH_TILE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
        WATER_GEL = block("water_gel", BlockWaterGel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(0.6f).sound(SoundType.SLIME_BLOCK));
        TUBE = block("tube", BlockTube::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1, 3_600_000).sound(SoundType.METAL)
                .noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE));

        TANK_TILE = tile("tank", TileTank::new, () -> TANK.get());
        MINING_WELL_TILE = tile("mining_well", TileMiningWell::new, () -> MINING_WELL.get());
        PUMP_TILE = tile("pump", TilePump::new, () -> PUMP.get());
        CHUTE_TILE = tile("chute", TileChute::new, () -> CHUTE.get());
        DISTILLER_TILE = tile("distiller", TileDistiller::new, () -> DISTILLER.get());
        HEAT_EXCHANGE_TILE = tile("heat_exchange", TileHeatExchange::new, () -> HEAT_EXCHANGE.get());
        AUTO_WORKBENCH_TILE = tile("autoworkbench_item", TileAutoWorkbench::new, () -> AUTO_WORKBENCH.get());
        FLOOD_GATE_TILE = tile("flood_gate", TileFloodGate::new, () -> FLOOD_GATE.get());
    }
}
