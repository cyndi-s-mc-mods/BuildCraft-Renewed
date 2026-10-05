package buildcraft.transport.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.lib.gui.GuiBC;
import buildcraft.transport.container.ContainerDiamondPipe;

public class GuiDiamondPipe extends GuiBC<ContainerDiamondPipe> {
    public GuiDiamondPipe(ContainerDiamondPipe menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/filter.png"), 175, 225);
        inventoryLabelY = imageHeight - 97;
    }
}
