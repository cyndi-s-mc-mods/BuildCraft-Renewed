package buildcraft.factory;

import net.minecraft.world.item.Item;

import buildcraft.factory.item.ItemWaterGel;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCFactoryItems {
    public static RegistryEntry<Item, ItemWaterGel> WATER_GEL_SPAWN;
    public static RegistryEntry<Item, Item> GEL;

    private BCFactoryItems() {}

    static void init() {
        WATER_GEL_SPAWN = item("water_gel_spawn", ItemWaterGel::new, () -> new Item.Properties().stacksTo(16));
        GEL = item("gel", Item::new);
    }
}
