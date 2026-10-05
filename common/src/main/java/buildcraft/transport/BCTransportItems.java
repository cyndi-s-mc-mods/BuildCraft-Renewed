package buildcraft.transport;

import net.minecraft.world.item.Item;

import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.plug.PluggablePowerAdaptor;

import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryEntry;

public final class BCTransportItems {
    /** Pipe sealant: turns transport pipes into fluid pipes. */
    public static RegistryEntry<Item, Item> PIPE_SEALANT;
    public static RegistryEntry<Item, ItemPluggableSimple> PLUG_BLOCKER;
    public static RegistryEntry<Item, ItemPluggableSimple> PLUG_POWER_ADAPTOR;

    private BCTransportItems() {}

    static void init() {
        PIPE_SEALANT = RegistrationHelper.item("pipe_sealant", Item::new);
        PLUG_BLOCKER = RegistrationHelper.item("plug_blocker",
            props -> new ItemPluggableSimple(props, () -> BCTransportPlugs.BLOCKER, PluggableBlocker::new, null));
        PLUG_POWER_ADAPTOR = RegistrationHelper.item("plug_power_adaptor",
            props -> new ItemPluggableSimple(props, () -> BCTransportPlugs.POWER_ADAPTOR, PluggablePowerAdaptor::new,
                (stack, holder, side) -> holder.getPipe().getBehaviour().getMjConnector(side) != null));
    }
}
