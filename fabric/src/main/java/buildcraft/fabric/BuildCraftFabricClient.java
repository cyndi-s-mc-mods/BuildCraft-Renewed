package buildcraft.fabric;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.api.ClientModInitializer;

import buildcraft.client.BCClient;
import buildcraft.client.ClientRegistrar;

public class BuildCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientRegistrar registrar = new ClientRegistrar() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRenderers.register(type, provider);
            }

            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(MenuType<? extends M> type,
                MenuScreens.ScreenConstructor<M, U> constructor) {
                MenuScreens.register(type, constructor);
            }
        };
        BCClient.registerRenderers(registrar);
        BCClient.registerScreens(registrar);
    }
}
