package buildcraft.forge;

import java.nio.file.Path;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraftforge.event.ForgeEventFactory;

import net.minecraftforge.fml.loading.FMLPaths;

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
    public int getBurnTime(ItemStack stack, int vanillaBurnTime) {
        return ForgeEventFactory.getItemBurnTime(stack, vanillaBurnTime, RecipeType.SMELTING);
    }
}
