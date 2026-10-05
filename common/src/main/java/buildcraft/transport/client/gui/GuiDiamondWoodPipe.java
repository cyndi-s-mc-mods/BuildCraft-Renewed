package buildcraft.transport.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.transport.container.ContainerDiamondWoodPipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;

public class GuiDiamondWoodPipe extends GuiBC<ContainerDiamondWoodPipe> {
    private static final Identifier BUTTONS = BuildCraft.id("textures/gui/pipe_emerald_button.png");
    private static final String[] TOOLTIPS = { "gui.buildcraft.pipe.emerald.whitelist", "gui.buildcraft.pipe.emerald.blacklist",
        "gui.buildcraft.pipe.emerald.roundrobin" };

    public GuiDiamondWoodPipe(ContainerDiamondWoodPipe menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/pipe_emerald.png"), 175, 161);
        inventoryLabelY = imageHeight - 93;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        FilterMode mode = menu.getMode();
        for (int i = 0; i < 3; i++) {
            int x = leftPos + 7 + i * 18, y = topPos + 41;
            boolean active = mode.ordinal() == i;
            boolean hover = isHovering(7 + i * 18, 41, 18, 18, mouseX, mouseY);
            // Button background: off, hovered and on states are stacked in the button texture
            int v = active ? 36 : hover ? 18 : 0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, x, y, 0, v, 18, 18, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, x + 1, y + 1, 19 + i * 18, 19, 16, 16, 256, 256);
        }
        if (mode == FilterMode.ROUND_ROBIN) {
            int slot = menu.currentFilter.getInt();
            blitPart(graphics, 6 + slot * 18, 16, 176, 0, 20, 20);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < 3; i++) {
            if (isHovering(7 + i * 18, 41, 18, 18, event.x(), event.y())) {
                sendButtonClick(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            if (isHovering(7 + i * 18, 41, 18, 18, mouseX, mouseY)) {
                showTooltip(graphics, List.of(Component.translatable(TOOLTIPS[i])), mouseX, mouseY);
            }
        }
    }
}
