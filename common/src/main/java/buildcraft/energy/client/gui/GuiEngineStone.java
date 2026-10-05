package buildcraft.energy.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.energy.container.ContainerEngineStone;
import buildcraft.lib.engine.GuiEngine;

public class GuiEngineStone extends GuiEngine<ContainerEngineStone> {
    public GuiEngineStone(ContainerEngineStone menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/steam_engine_gui.png"), 176, 166);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        double amount = menu.fuel.fuelLeft();
        if (amount > 0) {
            int flameHeight = (int) Math.ceil(amount * 14);
            blitPart(graphics, 81, 25 + 14 - flameHeight, 176, 14 - flameHeight, 14, flameHeight);
        }
    }

    @Override
    protected boolean isOverEngineInfo(double mouseX, double mouseY) {
        return isHovering(79, 23, 18, 18, mouseX, mouseY);
    }
}
