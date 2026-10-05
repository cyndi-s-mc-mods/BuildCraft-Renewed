package buildcraft.silicon;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.plug.PluggableLens;
import buildcraft.silicon.plug.PluggablePulsar;
import buildcraft.silicon.plug.PluggableSensor;
import buildcraft.api.transport.pipe.IPipeHolder;
import net.minecraft.core.Direction;

public final class BCSiliconPlugs {
    public static final PluggableDefinition GATE = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("gate"), PluggableGate::load));
    public static final PluggableDefinition PULSAR = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("pulsar"), PluggablePulsar::load));
    public static final PluggableDefinition LIGHT_SENSOR = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("light_sensor"), BCSiliconPlugs::lightSensor));
    public static final PluggableDefinition TIMER = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("timer"), BCSiliconPlugs::timer));
    public static final PluggableDefinition LENS = PluggableDefinition.register(
        new PluggableDefinition(BuildCraft.id("lens"), PluggableLens::load));

    public static PluggableSensor lightSensor(PluggableDefinition def, IPipeHolder holder, Direction side) {
        return new PluggableSensor(def, holder, side, BuildCraft.id("block/plugs/daylight_sensor"), () -> BCSiliconItems.PLUG_LIGHT_SENSOR.get());
    }

    public static PluggableSensor timer(PluggableDefinition def, IPipeHolder holder, Direction side) {
        return new PluggableSensor(def, holder, side, BuildCraft.id("block/plugs/timer"), () -> BCSiliconItems.PLUG_TIMER.get());
    }

    private BCSiliconPlugs() {}

    static void init() {}
}
