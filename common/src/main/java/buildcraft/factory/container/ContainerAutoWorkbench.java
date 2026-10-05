package buildcraft.factory.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.tile.TileAutoWorkbench;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotPhantom;

public class ContainerAutoWorkbench extends ContainerBC<TileAutoWorkbench> {
    public final MenuData.Field power;

    /** Client constructor. */
    public ContainerAutoWorkbench(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerAutoWorkbench(int id, Inventory inventory, @Nullable TileAutoWorkbench tile) {
        super(BCFactoryMenus.AUTO_WORKBENCH.get(), id, inventory, tile);
        Container result = tile != null ? tile.invResult : new SimpleContainer(1);
        Container blueprint = tile != null ? tile.invBlueprint : new SimpleContainer(9);
        Container materials = tile != null ? tile.invMaterials : new SimpleContainer(9);
        Container preview = tile != null ? tile.preview : new SimpleContainer(1);
        addSlot(new Slot(result, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(blueprint, x + y * 3, 30 + x * 18, 17 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(materials, x, 8 + x * 18, 84) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        addSlot(new Slot(preview, 0, 93, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        power = data.addLong(tile == null ? null : tile::getPowerStored);
        addPlayerInventory(115);
    }

    /** @return How far through the current craft the workbench is, from 0 to 1. */
    public double getProgress() {
        return Math.min(1, power.getLong() / (double) TileAutoWorkbench.POWER_REQUIRED);
    }
}
