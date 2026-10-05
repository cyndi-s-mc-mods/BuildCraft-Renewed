package buildcraft.client;

import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

import buildcraft.BuildCraft;
import buildcraft.client.render.RenderEngine;
import buildcraft.core.BCCoreBlocks;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.client.render.RenderQuarry;
import buildcraft.core.client.render.RenderMarkerVolume;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.client.gui.GuiAdvancedCraftingTable;
import buildcraft.silicon.client.gui.GuiAssemblyTable;
import buildcraft.silicon.client.gui.GuiGate;
import buildcraft.silicon.client.render.RenderLaser;
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
import buildcraft.silicon.client.render.FacadeClient;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.client.gui.GuiArchitectTable;
import buildcraft.builders.client.gui.GuiBuilder;
import buildcraft.builders.client.gui.GuiElectronicLibrary;
import buildcraft.builders.client.gui.GuiFiller;
import buildcraft.builders.client.render.RenderBuildBox;

/** Client-side setup. Each loader calls the methods here when its client registration events fire. */
public final class BCClient {
    private BCClient() {}

    public static void registerRenderers(ClientRegistrar registrar) {
        FacadeClient.init();
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE.get(), ctx -> new RenderEngine<>(ctx, "wood"));
        registrar.blockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE.get(), ctx -> new RenderEngine<>(ctx, "creative"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_STIRLING_TILE.get(), ctx -> new RenderEngine<>(ctx, "stone"));
        registrar.blockEntityRenderer(BCEnergyBlocks.ENGINE_COMBUSTION_TILE.get(), ctx -> new RenderEngine<>(ctx, "iron"));
        registrar.blockEntityRenderer(BCTransportBlocks.PIPE_HOLDER.get(), RenderPipeHolder::new);
        registrar.blockEntityRenderer(BCCoreBlocks.MARKER_VOLUME_TILE.get(), RenderMarkerVolume::new);
        registrar.blockEntityRenderer(BCBuildersBlocks.QUARRY_TILE.get(), RenderQuarry::new);
        registrar.blockEntityRenderer(BCBuildersBlocks.FILLER_TILE.get(), RenderBuildBox::new);
        registrar.blockEntityRenderer(BCBuildersBlocks.BUILDER_TILE.get(), RenderBuildBox::new);
        registrar.blockEntityRenderer(BCBuildersBlocks.ARCHITECT_TILE.get(), RenderBuildBox::new);
        registrar.blockEntityRenderer(BCSiliconBlocks.LASER_TILE.get(), RenderLaser::new);
        registrar.blockEntityRenderer(BCFactoryBlocks.TANK_TILE.get(), RenderTank::new);
        registrar.blockEntityRenderer(BCFactoryBlocks.DISTILLER_TILE.get(), RenderDistiller::new);
    }

    /** Special item renderers, for items whose looks depend on their data (such as facades). */
    public static void registerSpecialItemRenderers(BiConsumer<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> registry) {
        registry.accept(BuildCraft.id("facade"), FacadeClient.Unbaked.MAP_CODEC);
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
        registrar.screen(BCSiliconMenus.ASSEMBLY_TABLE.get(), GuiAssemblyTable::new);
        registrar.screen(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), GuiAdvancedCraftingTable::new);
        registrar.screen(BCSiliconMenus.GATE.get(), GuiGate::new);
        registrar.screen(BCBuildersMenus.FILLER.get(), GuiFiller::new);
        registrar.screen(BCBuildersMenus.ARCHITECT.get(), GuiArchitectTable::new);
        registrar.screen(BCBuildersMenus.BUILDER.get(), GuiBuilder::new);
        registrar.screen(BCBuildersMenus.LIBRARY.get(), GuiElectronicLibrary::new);
    }
}
