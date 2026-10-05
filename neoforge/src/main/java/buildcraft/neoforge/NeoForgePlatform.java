package buildcraft.neoforge;

import java.nio.file.Path;

import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.fml.loading.FMLPaths;

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
}
