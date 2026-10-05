package buildcraft.silicon.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotDisplay;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.tile.TileAssemblyTable;

public class ContainerAssemblyTable extends ContainerBC<TileAssemblyTable> {
    public final MenuData.Field power;
    public final MenuData.Field target;
    public final MenuData.Field[] states = new MenuData.Field[TileAssemblyTable.SLOTS];
    private int firstDisplaySlot;

    /** Client constructor. */
    public ContainerAssemblyTable(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerAssemblyTable(int id, Inventory inventory, @Nullable TileAssemblyTable tile) {
        super(BCSiliconMenus.ASSEMBLY_TABLE.get(), id, inventory, tile);
        Container inv = tile != null ? tile.inv : new SimpleContainer(TileAssemblyTable.SLOTS);
        Container display = tile != null ? tile.display : new SimpleContainer(TileAssemblyTable.SLOTS);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new Slot(inv, x + y * 3, 8 + x * 18, 36 + y * 18));
            }
        }
        firstDisplaySlot = slots.size();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotDisplay(display, x + y * 3, 116 + x * 18, 36 + y * 18));
            }
        }
        power = data.addLong(tile == null ? null : () -> tile.power);
        target = data.addLong(tile == null ? null : tile::getTarget);
        for (int i = 0; i < states.length; i++) {
            int index = i;
            states[i] = data.addInt(tile == null ? null : () -> tile.getDisplayState(index));
        }
        addPlayerInventory(123);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex >= firstDisplaySlot && slotIndex < firstDisplaySlot + TileAssemblyTable.SLOTS) {
            if (tile != null && input == ContainerInput.PICKUP) {
                tile.toggleRecipe(slotIndex - firstDisplaySlot);
            }
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }
}
