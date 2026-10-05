package buildcraft.lib.gui;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.Tank;

/** Base menu for BuildCraft machines. On the server {@link #tile} is the machine; on the client it is null and
 * everything the screen shows comes from synced slots and {@link MenuData}. */
public abstract class ContainerBC<T extends BlockEntity> extends AbstractContainerMenu {
    public final @Nullable T tile;
    public final MenuData data = new MenuData();
    protected final Inventory playerInventory;
    /** The number of machine slots, which come before the player's slots. */
    private int machineSlots = 0;

    protected ContainerBC(MenuType<?> type, int id, Inventory playerInventory, @Nullable T tile) {
        super(type, id);
        this.tile = tile;
        this.playerInventory = playerInventory;
    }

    /** Call after adding all machine slots and data fields. */
    protected void addPlayerInventory(int y) {
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, y + 58));
        }
        addDataSlots(data);
    }

    /** The contents of a tank, synced to the client. */
    public record TankView(MenuData.Field fluidId, MenuData.Field amount, int capacity) {
        public BCFluidStack getFluid() {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId.getInt());
            return fluid == null ? BCFluidStack.EMPTY : BCFluidStack.of(fluid, amount.getInt());
        }
    }

    /** Syncs a tank's contents to the client. Call in the same order on both sides; tank is null on the client. */
    protected TankView addTank(@Nullable Tank tank, int capacity) {
        MenuData.Field id = data.addInt(tank == null ? null : () -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid()));
        MenuData.Field amount = data.addInt(tank == null ? null : tank::getFluidAmount);
        return new TankView(id, amount, capacity);
    }

    @Override
    public boolean stillValid(Player player) {
        return tile == null || Container.stillValidBlockEntity(tile, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots;
        int playerEnd = slots.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machineSlots == 0 || !moveItemStackTo(stack, 0, machineSlots, false)) {
            // Move between the main inventory and the hotbar
            int hotbarStart = playerEnd - 9;
            if (index < hotbarStart) {
                if (!moveItemStackTo(stack, hotbarStart, playerEnd, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
