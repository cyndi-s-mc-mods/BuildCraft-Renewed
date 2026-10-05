package buildcraft.forge;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import buildcraft.client.BCClient;
import buildcraft.client.ClientRegistrar;
import buildcraft.lib.fluid.BCFluidDefinition;

/** Client-only setup. Only loaded on the physical client. */
final class BuildCraftForgeClient {
    private BuildCraftForgeClient() {}

    static void init(FMLJavaModLoadingContext context) {
        ModelEvent.BakeFluidModels.BUS.addListener(event -> {
            for (BCFluidDefinition def : BCFluidDefinition.ALL) {
                FluidModel model = BCClient.fluidModel(def).bake(event.materials(), def.source.get()::toString);
                event.register(def.source.get(), model);
                event.register(def.flowing.get(), model);
            }
        });
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> BCClient.registerRenderers(new ClientRegistrar() {
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
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> {
            List<Runnable> screens = new ArrayList<>();
            BCClient.registerScreens(new ClientRegistrar() {
                @Override
                public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(
                    MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> constructor) {
                    screens.add(() -> MenuScreens.register(type, constructor));
                }
            });
            event.enqueueWork(() -> screens.forEach(Runnable::run));
        });
    }
}
