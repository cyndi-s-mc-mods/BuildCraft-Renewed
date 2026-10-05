package buildcraft.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

import buildcraft.BuildCraft;
import buildcraft.client.render.RenderEngine;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.client.gui.GuiEngineIron;
import buildcraft.energy.client.gui.GuiEngineStone;
import buildcraft.lib.fluid.BCFluidDefinition;

/** Client-side setup. Each loader calls the methods here when its client registration events fire. */
public final class BCClient {
    private BCClient() {}

    public static void registerRenderers(ClientRegistrar registrar) {
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE.get(), ctx -> new RenderEngine<>(ctx, "wood"));
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE.get(), ctx -> new RenderEngine<>(ctx, "creative"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_STIRLING_TILE.get(), ctx -> new RenderEngine<>(ctx, "stone"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_COMBUSTION_TILE.get(), ctx -> new RenderEngine<>(ctx, "iron"));
    }

    /** @return The world model for a BuildCraft fluid. */
    public static FluidModel.Unbaked fluidModel(BCFluidDefinition def) {
        return new FluidModel.Unbaked(new Material(BuildCraft.id(def.stillTexture())), new Material(BuildCraft.id(def.flowTexture())),
            null, null);
    }

    public static void registerScreens(ClientRegistrar registrar) {
        registrar.screen(BCEnergyMenus.ENGINE_STIRLING.get(), GuiEngineStone::new);
        registrar.screen(BCEnergyMenus.ENGINE_COMBUSTION.get(), GuiEngineIron::new);
    }
}
