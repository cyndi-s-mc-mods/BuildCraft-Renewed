package buildcraft.factory.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.BuildCraft;
import buildcraft.factory.container.ContainerChute;
import buildcraft.lib.gui.GuiBC;

public class GuiChute extends GuiBC<ContainerChute> {
    public GuiChute(ContainerChute menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BuildCraft.id("textures/gui/chute.png"), 176, 153);
    }
}
