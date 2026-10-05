package buildcraft.forge;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraftforge.event.ForgeEventFactory;

import net.minecraftforge.fml.loading.FMLPaths;

import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.platform.Platform;

public class ForgePlatform implements Platform {
    @Override
    public String loaderName() {
        return "Forge";
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
    public FlowingFluid createFluid(BCFluidDefinition def, boolean source) {
        return ForgeFluids.create(def, source);
    }

    @Override
    public @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        return ForgeTransfer.getItemTransactor(level, pos, side);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        return ForgeTransfer.getFluidHandler(level, pos, side);
    }

    @Override
    public int getBurnTime(ItemStack stack, int vanillaBurnTime) {
        return ForgeEventFactory.getItemBurnTime(stack, vanillaBurnTime, RecipeType.SMELTING);
    }
}
