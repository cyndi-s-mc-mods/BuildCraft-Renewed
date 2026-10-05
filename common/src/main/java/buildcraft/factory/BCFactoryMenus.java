package buildcraft.factory;

import net.minecraft.world.inventory.MenuType;

import buildcraft.factory.container.ContainerChute;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCFactoryMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerChute>> CHUTE;

    private BCFactoryMenus() {}

    static void init() {
        CHUTE = menu("chute", ContainerChute::new);
    }
}
