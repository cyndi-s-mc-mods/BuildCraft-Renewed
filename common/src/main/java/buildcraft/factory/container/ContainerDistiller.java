package buildcraft.factory.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.tile.TileDistiller;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;

public class ContainerDistiller extends ContainerBC<TileDistiller> {
    public final TankView tankIn;
    public final TankView tankGasOut;
    public final TankView tankLiquidOut;
    public final MenuData.Field active;

    /** Client constructor. */
    public ContainerDistiller(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerDistiller(int id, Inventory inventory, @Nullable TileDistiller tile) {
        super(BCFactoryMenus.DISTILLER.get(), id, inventory, tile);
        tankIn = addTank(tile == null ? null : tile.tankIn, TileDistiller.CAPACITY);
        tankGasOut = addTank(tile == null ? null : tile.tankGasOut, TileDistiller.CAPACITY);
        tankLiquidOut = addTank(tile == null ? null : tile.tankLiquidOut, TileDistiller.CAPACITY);
        active = data.addBoolean(tile == null ? null : () -> tile.isActive);
        addPlayerInventory(79);
    }
}
