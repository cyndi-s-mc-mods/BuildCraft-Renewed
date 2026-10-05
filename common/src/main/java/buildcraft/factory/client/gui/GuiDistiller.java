package buildcraft.factory.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.GuiBC;

public class GuiDistiller extends GuiBC<ContainerDistiller> {
    /** x, y, width, height of each tank: input, gas output, liquid output. */
    private static final int[][] TANKS = { { 44, 23, 16, 38 }, { 98, 10, 34, 17 }, { 98, 54, 34, 17 } };

    public GuiDistiller(ContainerDistiller menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/distiller.png"), 176, 161);
    }

    private ContainerBC.TankView tank(int i) {
        return switch (i) {
            case 0 -> menu.tankIn;
            case 1 -> menu.tankGasOut;
            default -> menu.tankLiquidOut;
        };
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        if (menu.active.getBoolean()) {
            blitPart(graphics, 61, 12, 176, 57, 36, 57);
        }
        for (int i = 0; i < 3; i++) {
            int[] t = TANKS[i];
            drawTank(graphics, tank(i), t[0], t[1], t[2], t[3]);
        }
        // Gauge lines over the fluid
        blitPart(graphics, 44, 23, 0, 161, 16, 38);
        blitPart(graphics, 98, 10, 17, 161, 34, 17);
        blitPart(graphics, 98, 54, 17, 161, 34, 17);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        for (int i = 0; i < 3; i++) {
            int[] t = TANKS[i];
            if (isHovering(t[0], t[1], t[2], t[3], mouseX, mouseY)) {
                showTooltip(graphics, getTankTooltip(tank(i)), mouseX, mouseY);
            }
        }
    }
}
