package buildcraft.factory;

import net.minecraft.world.inventory.MenuType;

import buildcraft.factory.container.ContainerAutoWorkbench;
import buildcraft.factory.container.ContainerChute;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCFactoryMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerChute>> CHUTE;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerDistiller>> DISTILLER;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerAutoWorkbench>> AUTO_WORKBENCH;

    private BCFactoryMenus() {}

    static void init() {
        CHUTE = menu("chute", ContainerChute::new);
        DISTILLER = menu("distiller", ContainerDistiller::new);
        AUTO_WORKBENCH = menu("autoworkbench_item", ContainerAutoWorkbench::new);
    }
}
