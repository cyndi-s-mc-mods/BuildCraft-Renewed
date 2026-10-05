package buildcraft.silicon;

import net.minecraft.world.inventory.MenuType;

import buildcraft.lib.registry.RegistryEntry;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import buildcraft.silicon.container.ContainerAssemblyTable;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCSiliconMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerAssemblyTable>> ASSEMBLY_TABLE;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE;

    private BCSiliconMenus() {}

    static void init() {
        ASSEMBLY_TABLE = menu("assembly_table", ContainerAssemblyTable::new);
        ADVANCED_CRAFTING_TABLE = menu("advanced_crafting_table", ContainerAdvancedCraftingTable::new);
    }
}
