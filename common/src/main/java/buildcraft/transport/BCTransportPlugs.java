package buildcraft.transport;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.plug.PluggablePowerAdaptor;

public final class BCTransportPlugs {
    public static final PluggableDefinition BLOCKER = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("blocker"), PluggableBlocker::new));
    public static final PluggableDefinition POWER_ADAPTOR = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("power_adaptor"), PluggablePowerAdaptor::new));

    private BCTransportPlugs() {}

    static void init() {}
}
