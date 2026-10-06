package buildcraft.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Client-side registration, implemented by each loader using its own events. */
public interface ClientRegistrar {
    <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(BlockEntityType<? extends T> type,
        BlockEntityRendererProvider<T, S> provider);

    <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider);

    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void screen(MenuType<? extends M> type,
        MenuScreens.ScreenConstructor<M, U> constructor);
}
