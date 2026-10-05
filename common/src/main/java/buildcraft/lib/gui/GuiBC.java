package buildcraft.lib.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Base screen for BuildCraft machines, drawing a single background texture. */
public abstract class GuiBC<M extends ContainerBC<?>> extends AbstractContainerScreen<M> {
    protected final Identifier texture;

    protected GuiBC(M menu, Inventory inventory, Component title, Identifier texture, int width, int height) {
        super(menu, inventory, title, width, height);
        this.texture = texture;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        drawBackgroundLayer(graphics, partialTicks, mouseX, mouseY);
    }

    /** Draws machine-specific parts on top of the background. */
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {}

    /** Draws part of the background texture at a position relative to the GUI. */
    protected void blitPart(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + x, topPos + y, u, v, w, h, 256, 256);
    }

    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
