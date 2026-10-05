package buildcraft.lib.engine;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;

/** Base menu for engines, syncing the engine's heat, power and output for display. */
public abstract class ContainerEngine<T extends TileEngineBase> extends ContainerBC<T> {
    public final MenuData.Field heat;
    public final MenuData.Field power;
    public final MenuData.Field maxPower;
    public final MenuData.Field output;

    protected ContainerEngine(MenuType<?> type, int id, Inventory inventory, @Nullable T tile) {
        super(type, id, inventory, tile);
        heat = data.addDouble(tile == null ? null : tile::getHeat);
        power = data.addLong(tile == null ? null : tile::getEnergyStored);
        maxPower = data.addLong(tile == null ? null : tile::getMaxPower);
        output = data.addLong(tile == null ? null : () -> tile.currentOutput);
    }
}
