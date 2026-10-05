package buildcraft.silicon.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotDisplay;
import buildcraft.lib.gui.SlotPhantom;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;

public class ContainerAdvancedCraftingTable extends ContainerBC<TileAdvancedCraftingTable> {
    public final MenuData.Field power;
    public final MenuData.Field target;

    /** Client constructor. */
    public ContainerAdvancedCraftingTable(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerAdvancedCraftingTable(int id, Inventory inventory, @Nullable TileAdvancedCraftingTable tile) {
        super(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), id, inventory, tile);
        Container materials = tile != null ? tile.invMaterials : new SimpleContainer(15);
        Container results = tile != null ? tile.invResults : new SimpleContainer(9);
        Container blueprint = tile != null ? tile.invBlueprint : new SimpleContainer(9);
        Container preview = tile != null ? tile.preview : new SimpleContainer(1);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                addSlot(new Slot(materials, x + y * 5, 15 + x * 18, 85 + y * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new Slot(results, x + y * 3, 109 + x * 18, 85 + y * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(blueprint, x + y * 3, 33 + x * 18, 16 + y * 18));
            }
        }
        addSlot(new SlotDisplay(preview, 0, 127, 33));
        power = data.addLong(tile == null ? null : () -> tile.power);
        target = data.addLong(tile == null ? null : tile::getTarget);
        addPlayerInventory(153);
    }
}
