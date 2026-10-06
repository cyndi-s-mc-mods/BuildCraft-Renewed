package buildcraft.lib.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;

import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;

/** The loader-specific parts of BuildCraft. Each loader provides one implementation through {@link ServiceLoader}. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
        .orElseThrow(() -> new IllegalStateException("No BuildCraft platform implementation found"));

    String loaderName();

    Path configDir();

    CreativeModeTab.Builder creativeTabBuilder();

    /** Creates the fluid for a definition. Forge and NeoForge return subclasses that know their FluidType. */
    default FlowingFluid createFluid(BCFluidDefinition def, boolean source) {
        return source ? new BCFluid.Source(def) : new BCFluid.Flowing(def);
    }

    /** @param side The side of the block at pos that is being accessed.
     * @return Access to the items of the block at pos (from any mod), or null if it has no inventory. */
    @Nullable
    IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side);

    /** @param side The side of the block at pos that is being accessed.
     * @return Access to the tanks of the block at pos (from any mod), or null if it has none. */
    @Nullable
    IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side);

    /** Lets the loader adjust furnace burn times (Forge fires an event for this).
     * @param vanillaBurnTime The burn time from the item's cooking fuel component. */
    default int getBurnTime(ItemStack stack, int vanillaBurnTime) {
        return vanillaBurnTime;
    }

    /** Sends one of BuildCraft's packets (see {@link buildcraft.lib.net.BCNetwork}) to a player. */
    void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload);

    /** Sends one of BuildCraft's packets to the server. Only call this on the client. */
    void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload);
}
