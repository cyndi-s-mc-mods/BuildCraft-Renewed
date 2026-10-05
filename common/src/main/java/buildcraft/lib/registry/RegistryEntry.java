package buildcraft.lib.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** A registry object that is created and registered later, when the loader allows it. */
public final class RegistryEntry<R, T extends R> implements Supplier<T> {
    private final ResourceKey<? extends Registry<R>> registry;
    private final Identifier id;
    private final Function<ResourceKey<R>, T> factory;
    private T value;

    RegistryEntry(ResourceKey<? extends Registry<R>> registry, Identifier id, Function<ResourceKey<R>, T> factory) {
        this.registry = registry;
        this.id = id;
        this.factory = factory;
    }

    public Identifier id() {
        return id;
    }

    public ResourceKey<R> key() {
        return ResourceKey.create(registry, id);
    }

    public ResourceKey<? extends Registry<R>> registry() {
        return registry;
    }

    synchronized T create() {
        if (value == null) {
            value = factory.apply(key());
        }
        return value;
    }

    public boolean isRegistered() {
        return value != null;
    }

    /** Returns the registered object. If its registry hasn't been registered yet the object is created early (some
     * objects, such as liquid blocks, need objects from another registry while they are constructed) and the same
     * instance is registered later. */
    @Override
    public T get() {
        return create();
    }
}
