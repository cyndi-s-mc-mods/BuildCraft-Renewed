package buildcraft.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.api.ModInitializer;

import buildcraft.BuildCraft;
import buildcraft.lib.registry.BCRegistry;

public class BuildCraftFabric implements ModInitializer {
    @Override
    @SuppressWarnings("unchecked")
    public void onInitialize() {
        BuildCraft.init();
        // Fabric lets mods register straight away, so register everything in BuildCraft's own order.
        for (var key : BCRegistry.registries()) {
            Registry<Object> registry = (Registry<Object>) BuiltInRegistries.REGISTRY.getValue(key.identifier());
            BCRegistry.registerAll(key, (entry, value) -> Registry.register(registry, entry.id(), value));
        }
    }
}
