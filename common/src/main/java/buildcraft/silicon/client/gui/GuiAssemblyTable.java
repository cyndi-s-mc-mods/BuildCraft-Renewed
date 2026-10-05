package buildcraft.silicon.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.silicon.container.ContainerAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable;

public class GuiAssemblyTable extends GuiBC<ContainerAssemblyTable> {
    public GuiAssemblyTable(ContainerAssemblyTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/assembly_table.png"), 176, 220);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 15;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        long target = menu.target.getLong();
        if (target > 0) {
            double v = Math.min(1, menu.power.getLong() / (double) target);
            int h = (int) Math.ceil(70 * v);
            blitPart(graphics, 86, 36 + 70 - h, 176, 48 + 70 - h, 4, h);
        }
        for (int i = 0; i < menu.states.length; i++) {
            int state = menu.states[i].getInt();
            if (state <= TileAssemblyTable.State.POSSIBLE.ordinal()) continue;
            int x = 116 + (i % 3) * 18, y = 36 + (i / 3) * 18;
            blitPart(graphics, x, y, 176, (state - 1) * 16, 16, 16);
        }
    }
}
