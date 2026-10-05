package buildcraft.transport;

import net.minecraft.world.item.Item;

import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryEntry;

public final class BCTransportItems {
    /** Pipe sealant: turns transport pipes into fluid pipes. */
    public static RegistryEntry<Item, Item> PIPE_SEALANT;

    private BCTransportItems() {}

    static void init() {
        PIPE_SEALANT = RegistrationHelper.item("pipe_sealant", Item::new);
    }
}
