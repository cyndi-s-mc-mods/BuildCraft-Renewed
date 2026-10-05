package buildcraft.energy.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;

import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.tile.TileEngineIron;
import buildcraft.lib.engine.ContainerEngine;

public class ContainerEngineIron extends ContainerEngine<TileEngineIron> {
    public final TankView tankFuel;
    public final TankView tankCoolant;
    public final TankView tankResidue;

    /** Client constructor. */
    public ContainerEngineIron(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerEngineIron(int id, Inventory inventory, @Nullable TileEngineIron tile) {
        super(BCEnergyMenus.ENGINE_COMBUSTION.get(), id, inventory, tile);
        tankFuel = addTank(tile == null ? null : tile.tankFuel, TileEngineIron.MAX_FLUID);
        tankCoolant = addTank(tile == null ? null : tile.tankCoolant, TileEngineIron.MAX_FLUID);
        tankResidue = addTank(tile == null ? null : tile.tankResidue, TileEngineIron.MAX_FLUID);
        addPlayerInventory(95);
    }
}
