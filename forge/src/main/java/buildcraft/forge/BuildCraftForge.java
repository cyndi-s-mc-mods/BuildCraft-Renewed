package buildcraft.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

import buildcraft.BuildCraft;
import buildcraft.lib.registry.BCRegistry;

@Mod(BuildCraft.MOD_ID)
public class BuildCraftForge {
    public BuildCraftForge(FMLJavaModLoadingContext context) {
        BuildCraft.init();
        RegisterEvent.getBus(context.getModBusGroup()).addListener(BuildCraftForge::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BuildCraftForgeClient.init(context);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(RegisterEvent event) {
        ResourceKey<? extends Registry<Object>> key = (ResourceKey) event.getRegistryKey();
        BCRegistry.registerAll(key, (entry, value) -> event.register(key, entry.id(), () -> value));
    }
}
