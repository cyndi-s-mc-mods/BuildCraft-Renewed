package buildcraft.energy;

import net.minecraft.world.inventory.MenuType;

import buildcraft.energy.container.ContainerEngineIron;
import buildcraft.energy.container.ContainerEngineStone;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCEnergyMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerEngineStone>> ENGINE_STIRLING;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerEngineIron>> ENGINE_COMBUSTION;

    private BCEnergyMenus() {}

    static void init() {
        ENGINE_STIRLING = menu("engine_stirling", ContainerEngineStone::new);
        ENGINE_COMBUSTION = menu("engine_combustion", ContainerEngineIron::new);
    }
}
