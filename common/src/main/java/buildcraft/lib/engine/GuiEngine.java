package buildcraft.lib.engine;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.api.mj.MjAPI;
import buildcraft.lib.gui.GuiBC;

/** Base screen for engines. Hovering over the engine area shows its heat, stored power and output. */
public abstract class GuiEngine<M extends ContainerEngine<?>> extends GuiBC<M> {
    protected GuiEngine(M menu, Inventory inventory, Component title, Identifier texture, int width, int height) {
        super(menu, inventory, title, texture, width, height);
    }

    protected abstract boolean isOverEngineInfo(double mouseX, double mouseY);

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && isOverEngineInfo(mouseX, mouseY)) {
            List<Component> lines = List.of(
                Component.translatable("gui.buildcraft.engine.heat", String.format("%.1f", menu.heat.getDouble())),
                Component.translatable("gui.buildcraft.engine.stored", MjAPI.formatMj(menu.power.getLong()),
                    MjAPI.formatMj(menu.maxPower.getLong())),
                Component.translatable("gui.buildcraft.engine.output", MjAPI.formatMj(menu.output.getLong())));
            graphics.setTooltipForNextFrame(lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
        }
    }
}
