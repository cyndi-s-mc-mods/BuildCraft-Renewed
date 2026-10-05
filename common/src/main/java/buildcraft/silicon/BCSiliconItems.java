package buildcraft.silicon;

import net.minecraft.world.item.Item;

import buildcraft.lib.registry.RegistryEntry;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.item.ItemPluggableLens;
import buildcraft.silicon.plug.PluggablePulsar;

import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCSiliconItems {
    public static RegistryEntry<Item, Item> CHIPSET_REDSTONE;
    public static RegistryEntry<Item, Item> CHIPSET_IRON;
    public static RegistryEntry<Item, Item> CHIPSET_GOLD;
    public static RegistryEntry<Item, Item> CHIPSET_QUARTZ;
    public static RegistryEntry<Item, Item> CHIPSET_DIAMOND;
    public static RegistryEntry<Item, ItemPluggableGate> GATE;
    public static RegistryEntry<Item, ItemPluggableSimple> PLUG_PULSAR;
    public static RegistryEntry<Item, ItemPluggableSimple> PLUG_LIGHT_SENSOR;
    public static RegistryEntry<Item, ItemPluggableSimple> PLUG_TIMER;
    public static RegistryEntry<Item, ItemPluggableLens> LENS;
    public static RegistryEntry<Item, ItemPluggableFacade> FACADE;
    public static RegistryEntry<Item, ItemPluggableLens> FILTER;

    private BCSiliconItems() {}

    static void init() {
        CHIPSET_REDSTONE = item("chipset_redstone", Item::new);
        CHIPSET_IRON = item("chipset_iron", Item::new);
        CHIPSET_GOLD = item("chipset_gold", Item::new);
        CHIPSET_QUARTZ = item("chipset_quartz", Item::new);
        CHIPSET_DIAMOND = item("chipset_diamond", Item::new);
        GATE = item("gate", ItemPluggableGate::new);
        PLUG_PULSAR = item("plug_pulsar", props -> new ItemPluggableSimple(props, () -> BCSiliconPlugs.PULSAR, PluggablePulsar::new,
            (stack, holder, side) -> holder.getPipe().getBehaviour() instanceof IMjRedstoneReceiver));
        PLUG_LIGHT_SENSOR = item("plug_light_sensor", props -> new ItemPluggableSimple(props, () -> BCSiliconPlugs.LIGHT_SENSOR,
            BCSiliconPlugs::lightSensor, null));
        PLUG_TIMER = item("plug_timer", props -> new ItemPluggableSimple(props, () -> BCSiliconPlugs.TIMER, BCSiliconPlugs::timer, null));
        LENS = item("lens", props -> new ItemPluggableLens(props, false));
        FILTER = item("filter", props -> new ItemPluggableLens(props, true));
        FACADE = item("facade", ItemPluggableFacade::new);
    }
}
