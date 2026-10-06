package buildcraft.transport.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.transport.container.ContainerEmzuliPipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;

public class GuiEmzuliPipe extends GuiBC<ContainerEmzuliPipe> {
    private static final int[][] BUTTONS = { { 49, 19 }, { 49, 47 }, { 106, 19 }, { 106, 47 } };

    public GuiEmzuliPipe(ContainerEmzuliPipe menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/pipe_emzuli.png"), 176, 166);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        int current = menu.currentSlot.getInt();
        for (SlotIndex index : SlotIndex.VALUES) {
            int[] pos = BUTTONS[index.ordinal()];
            blitPart(graphics, pos[0], pos[1], 176, index.ordinal() == current ? 20 : 0, 20, 20);
            int colour = menu.colours[index.ordinal()].getInt();
            if (colour < 0) {
                blitPart(graphics, pos[0] + 2, pos[1] + 2, 176, 40, 16, 16);
            } else {
                int rgb = DyeColor.byId(colour).getTextureDiffuseColor();
                graphics.fill(leftPos + pos[0] + 3, topPos + pos[1] + 3, leftPos + pos[0] + 17, topPos + pos[1] + 17, 0xFF000000 | rgb);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (SlotIndex index : SlotIndex.VALUES) {
            int[] pos = BUTTONS[index.ordinal()];
            if (isHovering(pos[0], pos[1], 20, 20, event.x(), event.y())) {
                int colour = menu.colours[index.ordinal()].getInt();
                // Cycle through no paint and the 16 colours; right click goes backwards
                int next = containerButton(event) == 1 ? colour - 1 : colour + 1;
                if (next > 15) next = -1;
                if (next < -1) next = 15;
                sendButtonClick(index.ordinal() * 17 + next + 1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (SlotIndex index : SlotIndex.VALUES) {
            int[] pos = BUTTONS[index.ordinal()];
            if (isHovering(pos[0], pos[1], 20, 20, mouseX, mouseY)) {
                int colour = menu.colours[index.ordinal()].getInt();
                Component name = colour < 0 ? Component.translatable("gui.buildcraft.pipe.emzuli.nopaint")
                    : Component.translatable("color.minecraft." + DyeColor.byId(colour).getSerializedName());
                showTooltip(graphics, List.of(name), mouseX, mouseY);
            }
        }
    }
}
