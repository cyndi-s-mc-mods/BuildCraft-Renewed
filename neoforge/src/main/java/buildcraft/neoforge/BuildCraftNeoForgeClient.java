package buildcraft.neoforge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import buildcraft.BuildCraft;
import buildcraft.client.BCClient;
import buildcraft.client.ClientRegistrar;
import buildcraft.lib.fluid.BCFluidDefinition;

@Mod(value = BuildCraft.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftNeoForgeClient {
    public BuildCraftNeoForgeClient(IEventBus modBus) {
        modBus.addListener(RegisterSpecialModelRendererEvent.class, event -> BCClient.registerSpecialItemRenderers(event::register));
        modBus.addListener(RegisterFluidModelsEvent.class, event -> {
            for (BCFluidDefinition def : BCFluidDefinition.ALL) {
                event.register(BCClient.fluidModel(def), def.source.get(), def.flowing.get());
            }
        });
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> BCClient.registerRenderers(new ClientRegistrar() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }

            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(MenuType<? extends M> type,
                MenuScreens.ScreenConstructor<M, U> constructor) {
                throw new UnsupportedOperationException();
            }
        }));
        modBus.addListener(RegisterMenuScreensEvent.class, event -> BCClient.registerScreens(new ClientRegistrar() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(MenuType<? extends M> type,
                MenuScreens.ScreenConstructor<M, U> constructor) {
                event.register(type, constructor);
            }
        }));
    }
}
