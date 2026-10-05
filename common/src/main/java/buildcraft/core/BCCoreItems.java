package buildcraft.core;

import net.minecraft.world.item.Item;

import buildcraft.core.item.ItemWrench;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCCoreItems {
    public static RegistryEntry<Item, ItemWrench> WRENCH;
    public static RegistryEntry<Item, Item> GEAR_WOOD;
    public static RegistryEntry<Item, Item> GEAR_STONE;
    public static RegistryEntry<Item, Item> GEAR_IRON;
    public static RegistryEntry<Item, Item> GEAR_GOLD;
    public static RegistryEntry<Item, Item> GEAR_DIAMOND;

    private BCCoreItems() {}

    static void init() {
        WRENCH = item("wrench", props -> new ItemWrench(props.stacksTo(1)));
        GEAR_WOOD = item("gear_wood", Item::new);
        GEAR_STONE = item("gear_stone", Item::new);
        GEAR_IRON = item("gear_iron", Item::new);
        GEAR_GOLD = item("gear_gold", Item::new);
        GEAR_DIAMOND = item("gear_diamond", Item::new);
    }
}
