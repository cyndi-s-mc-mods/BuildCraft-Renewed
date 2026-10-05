package buildcraft.silicon;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.silicon.plug.PluggableGate;

public final class BCSiliconPlugs {
    public static final PluggableDefinition GATE = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("gate"), PluggableGate::load));

    private BCSiliconPlugs() {}

    static void init() {}
}
