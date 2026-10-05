package buildcraft.lib.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import buildcraft.BuildCraft;

/** Collects every registry object BuildCraft adds. Each loader registers them at the point it allows. */
public final class BCRegistry {
    private static final Map<ResourceKey<? extends Registry<?>>, List<RegistryEntry<?, ?>>> ENTRIES = new LinkedHashMap<>();

    static {
        // Fixed order for loaders that register everything at once: later registries refer to earlier ones.
        ENTRIES.put(Registries.FLUID, new ArrayList<>());
        ENTRIES.put(Registries.BLOCK, new ArrayList<>());
        ENTRIES.put(Registries.ITEM, new ArrayList<>());
    }

    private BCRegistry() {}

    public static <R, T extends R> RegistryEntry<R, T> register(ResourceKey<? extends Registry<R>> registry, String name,
        Function<ResourceKey<R>, T> factory) {
        RegistryEntry<R, T> entry = new RegistryEntry<>(registry, BuildCraft.id(name), factory);
        ENTRIES.computeIfAbsent(registry, k -> new ArrayList<>()).add(entry);
        return entry;
    }

    /** @return every registry that BuildCraft adds entries to, in registration order. */
    public static Set<ResourceKey<? extends Registry<?>>> registries() {
        return ENTRIES.keySet();
    }

    /** Creates every entry for the given registry and passes it to the loader's registration callback. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void registerAll(ResourceKey<? extends Registry<?>> registry, BiConsumer<RegistryEntry<?, ?>, Object> sink) {
        List<RegistryEntry<?, ?>> list = ENTRIES.get(registry);
        if (list == null) return;
        for (RegistryEntry entry : list) {
            sink.accept(entry, entry.create());
        }
    }
}
