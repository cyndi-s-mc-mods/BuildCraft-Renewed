package buildcraft.fabric;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;

import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.platform.Platform;

public class FabricPlatform implements Platform {
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricCreativeModeTab.builder();
    }

    @Override
    public @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        return FabricTransfer.getItemTransactor(level, pos, side);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        return FabricTransfer.getFluidHandler(level, pos, side);
    }
}
