package buildcraft.client;

import buildcraft.client.render.RenderEngine;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.client.gui.GuiEngineStone;

/** Client-side setup. Each loader calls the methods here when its client registration events fire. */
public final class BCClient {
    private BCClient() {}

    public static void registerRenderers(ClientRegistrar registrar) {
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE.get(), ctx -> new RenderEngine<>(ctx, "wood"));
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE.get(), ctx -> new RenderEngine<>(ctx, "creative"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_STIRLING_TILE.get(), ctx -> new RenderEngine<>(ctx, "stone"));
    }

    public static void registerScreens(ClientRegistrar registrar) {
        registrar.screen(BCEnergyMenus.ENGINE_STIRLING.get(), GuiEngineStone::new);
    }
}
