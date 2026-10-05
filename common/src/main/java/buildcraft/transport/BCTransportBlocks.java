package buildcraft.transport;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryEntry;
import buildcraft.transport.tile.TilePipeHolder;

public final class BCTransportBlocks {
    public static RegistryEntry<BlockEntityType<?>, BlockEntityType<TilePipeHolder>> PIPE_HOLDER;

    private BCTransportBlocks() {}

    @SuppressWarnings("unchecked")
    static void init() {
        Supplier<? extends Block>[] blocks = BCTransportPipes.BLOCKS.values().toArray(new Supplier[0]);
        PIPE_HOLDER = RegistrationHelper.tile("pipe_holder", TilePipeHolder::new, blocks);
    }
}
