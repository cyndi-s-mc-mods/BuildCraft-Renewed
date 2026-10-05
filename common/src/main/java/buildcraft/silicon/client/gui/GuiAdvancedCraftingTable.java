package buildcraft.silicon.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;

public class GuiAdvancedCraftingTable extends GuiBC<ContainerAdvancedCraftingTable> {
    public GuiAdvancedCraftingTable(ContainerAdvancedCraftingTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/advanced_crafting_table.png"), 176, 241);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 5;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        long target = menu.target.getLong();
        if (target > 0) {
            double v = Math.min(1, menu.power.getLong() / (double) target);
            int h = (int) Math.ceil(70 * v);
            blitPart(graphics, 164, 7 + 70 - h, 176, 70 - h, 4, h);
        }
    }
}
