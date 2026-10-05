package buildcraft.energy.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.tile.TileEngineStone;
import buildcraft.lib.engine.ContainerEngine;

public class ContainerEngineStone extends ContainerEngine<TileEngineStone> {
    public final MenuDataFields fuel;

    /** Client constructor. */
    public ContainerEngineStone(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerEngineStone(int id, Inventory inventory, @Nullable TileEngineStone tile) {
        super(BCEnergyMenus.ENGINE_STIRLING.get(), id, inventory, tile);
        Container fuelInv = tile != null ? tile : new SimpleContainer(1);
        addSlot(new Slot(fuelInv, 0, 80, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return fuelInv.canPlaceItem(0, stack);
            }
        });
        fuel = new MenuDataFields(
            data.addInt(tile == null ? null : () -> tile.burnTime),
            data.addInt(tile == null ? null : () -> tile.totalBurnTime));
        addPlayerInventory(84);
    }

    public record MenuDataFields(buildcraft.lib.gui.MenuData.Field burnTime, buildcraft.lib.gui.MenuData.Field totalBurnTime) {
        /** @return How much of the current fuel item is left, from 0 to 1. */
        public double fuelLeft() {
            int total = totalBurnTime.getInt();
            return total <= 0 ? 0 : burnTime.getInt() / (double) total;
        }
    }
}
