package buildcraft.lib.inventory;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/** Access to an inventory for moving items in and out, without caring about its slots. Used for other mods'
 * inventories (through the platform) as well as BuildCraft's own. */
public interface IItemTransactor {
    /** @return The items that could not be inserted. */
    ItemStack insert(ItemStack stack, boolean simulate);

    /** Extracts a single stack of items that match the filter.
     * @return The extracted stack, or empty if fewer than min items could be extracted. */
    ItemStack extract(Predicate<ItemStack> filter, int min, int max, boolean simulate);
}
