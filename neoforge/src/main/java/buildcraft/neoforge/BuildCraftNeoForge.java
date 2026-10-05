package buildcraft.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

import buildcraft.BuildCraft;
import buildcraft.lib.registry.BCRegistry;

@Mod(BuildCraft.MOD_ID)
public class BuildCraftNeoForge {
    public BuildCraftNeoForge(IEventBus modBus) {
        BuildCraft.init();
        modBus.addListener(RegisterEvent.class, BuildCraftNeoForge::register);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(RegisterEvent event) {
        ResourceKey<? extends Registry<Object>> key = (ResourceKey) event.getRegistryKey();
        BCRegistry.registerAll(key, (entry, value) -> event.register(key, entry.id(), () -> value));
    }
}
