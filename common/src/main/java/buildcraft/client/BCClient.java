package buildcraft.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

import buildcraft.BuildCraft;
import buildcraft.client.render.RenderEngine;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.client.gui.GuiAutoWorkbench;
import buildcraft.factory.client.gui.GuiChute;
import buildcraft.factory.client.gui.GuiDistiller;
import buildcraft.factory.client.render.RenderDistiller;
import buildcraft.factory.client.render.RenderTank;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.client.gui.GuiEngineIron;
import buildcraft.energy.client.gui.GuiEngineStone;
import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.client.gui.GuiDiamondPipe;
import buildcraft.transport.client.gui.GuiDiamondWoodPipe;
import buildcraft.transport.client.gui.GuiEmzuliPipe;
import buildcraft.transport.client.render.RenderPipeHolder;

/** Client-side setup. Each loader calls the methods here when its client registration events fire. */
public final class BCClient {
    private BCClient() {}

    public static void registerRenderers(ClientRegistrar registrar) {
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE.get(), ctx -> new RenderEngine<>(ctx, "wood"));
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE.get(), ctx -> new RenderEngine<>(ctx, "creative"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_STIRLING_TILE.get(), ctx -> new RenderEngine<>(ctx, "stone"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_COMBUSTION_TILE.get(), ctx -> new RenderEngine<>(ctx, "iron"));
        registrar.blockEntityRenderer(BCTransportBlocks.PIPE_HOLDER.get(), RenderPipeHolder::new);
        registrar.blockEntityRenderer(BCFactoryBlocks.TANK_TILE.get(), RenderTank::new);
        registrar.blockEntityRenderer(BCFactoryBlocks.DISTILLER_TILE.get(), RenderDistiller::new);
    }

    /** @return The world model for a BuildCraft fluid. */
    public static FluidModel.Unbaked fluidModel(BCFluidDefinition def) {
        return new FluidModel.Unbaked(new Material(BuildCraft.id(def.stillTexture())), new Material(BuildCraft.id(def.flowTexture())),
            null, null);
    }

    public static void registerScreens(ClientRegistrar registrar) {
        registrar.screen(BCEnergyMenus.ENGINE_STIRLING.get(), GuiEngineStone::new);
        registrar.screen(BCEnergyMenus.ENGINE_COMBUSTION.get(), GuiEngineIron::new);
        registrar.screen(BCTransportMenus.PIPE_DIAMOND.get(), GuiDiamondPipe::new);
        registrar.screen(BCTransportMenus.PIPE_DIAMOND_WOOD.get(), GuiDiamondWoodPipe::new);
        registrar.screen(BCTransportMenus.PIPE_EMZULI.get(), GuiEmzuliPipe::new);
        registrar.screen(BCFactoryMenus.CHUTE.get(), GuiChute::new);
        registrar.screen(BCFactoryMenus.DISTILLER.get(), GuiDistiller::new);
        registrar.screen(BCFactoryMenus.AUTO_WORKBENCH.get(), GuiAutoWorkbench::new);
    }
}
