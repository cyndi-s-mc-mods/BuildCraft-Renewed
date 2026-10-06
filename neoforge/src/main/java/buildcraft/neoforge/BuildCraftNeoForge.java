package buildcraft.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.neoforged.neoforge.registries.NeoForgeRegistries;

import buildcraft.BuildCraft;
import buildcraft.lib.registry.BCRegistry;

@Mod(BuildCraft.MOD_ID)
public class BuildCraftNeoForge {
    public BuildCraftNeoForge(IEventBus modBus) {
        BuildCraft.init();
        modBus.addListener(RegisterEvent.class, BuildCraftNeoForge::register);
        modBus.addListener(RegisterCapabilitiesEvent.class, NeoForgeTransfer::registerCapabilities);
        modBus.addListener(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class, NeoForgeNetwork::register);
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(BuildCraft::setup));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(RegisterEvent event) {
        ResourceKey<? extends Registry<Object>> key = (ResourceKey) event.getRegistryKey();
        BCRegistry.registerAll(key, (entry, value) -> event.register(key, entry.id(), () -> value));
        NeoForgeFluids.registerTypes(key, (name, type) -> event.register(NeoForgeRegistries.Keys.FLUID_TYPES, BuildCraft.id(name), () -> type));
    }
}
