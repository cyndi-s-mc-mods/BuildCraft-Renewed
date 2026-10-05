package buildcraft.neoforge;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;

import net.neoforged.fml.loading.FMLPaths;

import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.platform.Platform;

public class NeoForgePlatform implements Platform {
    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        return NeoForgeTransfer.getItemTransactor(level, pos, side);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        return NeoForgeTransfer.getFluidHandler(level, pos, side);
    }

    @Override
    public FlowingFluid createFluid(BCFluidDefinition def, boolean source) {
        return NeoForgeFluids.create(def, source);
    }
}
