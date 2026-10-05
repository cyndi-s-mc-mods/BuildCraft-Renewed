package buildcraft.lib.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import buildcraft.lib.client.FluidRenderUtil;
import buildcraft.lib.fluid.BCFluidStack;

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
        // Put the "Inventory" label just above the player's inventory, wherever it is
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                inventoryLabelX = slot.x - 1;
                inventoryLabelY = slot.y - 11;
                break;
            }
        }
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

    /** Draws the fluid in a tank, filling the given area (relative to the GUI) from the bottom up. */
    protected void drawTank(GuiGraphicsExtractor graphics, ContainerBC.TankView tank, int x, int y, int w, int h) {
        BCFluidStack fluid = tank.getFluid();
        if (fluid.isEmpty() || tank.capacity() <= 0) return;
        int height = Math.max(1, Math.min(h, (int) Math.ceil(h * fluid.getAmount() / (double) tank.capacity())));
        TextureAtlasSprite sprite = FluidRenderUtil.getStillSprite(fluid.getFluid());
        int colour = FluidRenderUtil.getColour(fluid.getFluid());
        int x0 = leftPos + x, y1 = topPos + y + h, y0 = y1 - height;
        graphics.enableScissor(x0, y0, x0 + w, y1);
        for (int ty = y1 - 16; ty > y0 - 16; ty -= 16) {
            for (int tx = x0; tx < x0 + w; tx += 16) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, colour);
            }
        }
        graphics.disableScissor();
    }

    /** @return The tooltip for a tank: its fluid and how full it is. */
    protected List<Component> getTankTooltip(ContainerBC.TankView tank) {
        BCFluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return List.of(Component.translatable("gui.buildcraft.tank.empty", tank.capacity()));
        }
        return List.of(fluid.getName(), Component.translatable("gui.buildcraft.tank.amount", fluid.getAmount(), tank.capacity()));
    }

    protected void showTooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    /** Tells the server's menu that a button was pressed (see {@link net.minecraft.world.inventory.AbstractContainerMenu#clickMenuButton}). */
    protected void sendButtonClick(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
