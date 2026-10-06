package buildcraft.core;

import net.minecraft.world.item.Item;

import buildcraft.core.item.ItemFragileFluidShard;
import buildcraft.core.item.ItemGoggles;
import buildcraft.core.item.ItemList;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.item.ItemVolumeBox;
import buildcraft.core.item.ItemPaintbrush;
import buildcraft.core.item.ItemWrench;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.hiddenItem;
import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCCoreItems {
    public static RegistryEntry<Item, ItemWrench> WRENCH;
    public static RegistryEntry<Item, Item> GEAR_WOOD;
    public static RegistryEntry<Item, Item> GEAR_STONE;
    public static RegistryEntry<Item, Item> GEAR_IRON;
    public static RegistryEntry<Item, Item> GEAR_GOLD;
    public static RegistryEntry<Item, Item> GEAR_DIAMOND;
    public static RegistryEntry<Item, ItemPaintbrush> PAINTBRUSH;
    public static RegistryEntry<Item, ItemList> LIST;
    public static RegistryEntry<Item, ItemMarkerConnector> MARKER_CONNECTOR;
    public static RegistryEntry<Item, ItemVolumeBox> VOLUME_BOX;
    public static RegistryEntry<Item, ItemFragileFluidShard> FRAGILE_FLUID_SHARD;
    public static RegistryEntry<Item, ItemGoggles> GOGGLES;
    public static RegistryEntry<Item, ItemMapLocation> MAP_LOCATION;
    public static RegistryEntry<Item, buildcraft.lib.item.ItemGuide> GUIDE;

    private BCCoreItems() {}

    static void init() {
        WRENCH = item("wrench", props -> new ItemWrench(props.stacksTo(1)));
        GEAR_WOOD = item("gear_wood", Item::new);
        GEAR_STONE = item("gear_stone", Item::new);
        GEAR_IRON = item("gear_iron", Item::new);
        GEAR_GOLD = item("gear_gold", Item::new);
        GEAR_DIAMOND = item("gear_diamond", Item::new);
        PAINTBRUSH = item("paintbrush", ItemPaintbrush::new);
        LIST = item("list", props -> new ItemList(props.stacksTo(1)));
        MARKER_CONNECTOR = item("marker_connector", ItemMarkerConnector::new);
        VOLUME_BOX = item("volume_box", ItemVolumeBox::new);
        // Never shown in the creative tab, as there would be one for every fluid
        FRAGILE_FLUID_SHARD = hiddenItem("fragile_fluid_shard", ItemFragileFluidShard::new);
        GOGGLES = item("goggles", ItemGoggles::new);
        MAP_LOCATION = item("map_location", ItemMapLocation::new);
        GUIDE = item("guide", buildcraft.lib.item.ItemGuide::new);
    }
}
