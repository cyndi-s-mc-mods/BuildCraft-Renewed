package buildcraft.factory.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.tile.TileChute;
import buildcraft.lib.gui.ContainerBC;

public class ContainerChute extends ContainerBC<TileChute> {
    /** Client constructor. */
    public ContainerChute(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerChute(int id, Inventory inventory, @Nullable TileChute tile) {
        super(BCFactoryMenus.CHUTE.get(), id, inventory, tile);
        Container inv = tile != null ? tile.inv : new SimpleContainer(4);
        addSlot(new Slot(inv, 0, 62, 18));
        addSlot(new Slot(inv, 1, 80, 18));
        addSlot(new Slot(inv, 2, 98, 18));
        addSlot(new Slot(inv, 3, 80, 36));
        addPlayerInventory(71);
    }
}
