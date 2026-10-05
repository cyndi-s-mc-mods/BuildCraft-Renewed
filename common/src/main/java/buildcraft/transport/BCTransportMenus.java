package buildcraft.transport;

import net.minecraft.world.inventory.MenuType;

import buildcraft.lib.registry.RegistryEntry;
import buildcraft.transport.container.ContainerDiamondPipe;
import buildcraft.transport.container.ContainerDiamondWoodPipe;
import buildcraft.transport.container.ContainerEmzuliPipe;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCTransportMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerDiamondPipe>> PIPE_DIAMOND;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerDiamondWoodPipe>> PIPE_DIAMOND_WOOD;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerEmzuliPipe>> PIPE_EMZULI;

    private BCTransportMenus() {}

    static void init() {
        PIPE_DIAMOND = menu("pipe_diamond", ContainerDiamondPipe::new);
        PIPE_DIAMOND_WOOD = menu("pipe_diamond_wood", ContainerDiamondWoodPipe::new);
        PIPE_EMZULI = menu("pipe_emzuli", ContainerEmzuliPipe::new);
    }
}
