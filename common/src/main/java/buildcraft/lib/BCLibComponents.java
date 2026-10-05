package buildcraft.lib;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;

import buildcraft.lib.list.ListData;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;

public final class BCLibComponents {
    /** A colour, for items like lenses and paintbrushes. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<DyeColor>> COLOUR;

    /** What a list item matches. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<ListData>> LIST;

    private BCLibComponents() {}

    static void init() {
        COLOUR = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "colour",
            key -> DataComponentType.<DyeColor> builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build());
        LIST = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "list",
            key -> DataComponentType.<ListData> builder().persistent(ListData.CODEC).networkSynchronized(ListData.STREAM_CODEC).build());
    }
}
