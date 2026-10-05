package buildcraft.energy.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.energy.container.ContainerEngineIron;
import buildcraft.lib.engine.GuiEngine;
import buildcraft.lib.gui.ContainerBC;

public class GuiEngineIron extends GuiEngine<ContainerEngineIron> {
    private static final int[] TANK_X = { 26, 80, 134 };
    private static final int TANK_Y = 18, TANK_W = 16, TANK_H = 60;

    public GuiEngineIron(ContainerEngineIron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/combustion_engine_gui.png"), 176, 177);
    }

    private ContainerBC.TankView tank(int i) {
        return switch (i) {
            case 0 -> menu.tankFuel;
            case 1 -> menu.tankCoolant;
            default -> menu.tankResidue;
        };
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        for (int i = 0; i < 3; i++) {
            drawTank(graphics, tank(i), TANK_X[i], TANK_Y, TANK_W, TANK_H);
            // Gauge lines over the fluid
            blitPart(graphics, TANK_X[i], TANK_Y, 176, 0, TANK_W, TANK_H);
        }
    }

    @Override
    protected boolean isOverEngineInfo(double mouseX, double mouseY) {
        return isHovering(44, 18, 34, 60, mouseX, mouseY) || isHovering(98, 18, 34, 60, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        for (int i = 0; i < 3; i++) {
            if (isHovering(TANK_X[i], TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
                showTooltip(graphics, getTankTooltip(tank(i)), mouseX, mouseY);
            }
        }
    }
}
