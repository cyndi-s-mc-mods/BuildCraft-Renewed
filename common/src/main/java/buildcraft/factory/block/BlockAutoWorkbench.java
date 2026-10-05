package buildcraft.factory.block;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import buildcraft.factory.tile.TileAutoWorkbench;
import buildcraft.lib.block.BlockBCTile;

public class BlockAutoWorkbench extends BlockBCTile<TileAutoWorkbench> {
    public BlockAutoWorkbench(Properties properties, Supplier<BlockEntityType<TileAutoWorkbench>> tileType) {
        super(properties, tileType);
    }
}
