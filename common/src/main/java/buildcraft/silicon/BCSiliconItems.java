package buildcraft.silicon;

import net.minecraft.world.item.Item;

import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCSiliconItems {
    public static RegistryEntry<Item, Item> CHIPSET_REDSTONE;
    public static RegistryEntry<Item, Item> CHIPSET_IRON;
    public static RegistryEntry<Item, Item> CHIPSET_GOLD;
    public static RegistryEntry<Item, Item> CHIPSET_QUARTZ;
    public static RegistryEntry<Item, Item> CHIPSET_DIAMOND;

    private BCSiliconItems() {}

    static void init() {
        CHIPSET_REDSTONE = item("chipset_redstone", Item::new);
        CHIPSET_IRON = item("chipset_iron", Item::new);
        CHIPSET_GOLD = item("chipset_gold", Item::new);
        CHIPSET_QUARTZ = item("chipset_quartz", Item::new);
        CHIPSET_DIAMOND = item("chipset_diamond", Item::new);
    }
}
