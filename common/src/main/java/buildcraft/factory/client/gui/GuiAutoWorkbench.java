package buildcraft.factory.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.factory.container.ContainerAutoWorkbench;
import buildcraft.lib.gui.GuiBC;

public class GuiAutoWorkbench extends GuiBC<ContainerAutoWorkbench> {
    public GuiAutoWorkbench(ContainerAutoWorkbench menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/autobench_item.png"), 176, 197);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        int width = (int) Math.round(menu.getProgress() * 23);
        if (width > 0) {
            blitPart(graphics, 90, 47, 176, 0, width, 10);
        }
    }
}
