package buildcraft.lib.inventory;

import java.util.function.BiPredicate;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A simple inventory for block entities, with a filter for which items each slot accepts. */
public class ItemHandlerSimple extends SimpleContainer {
    private final BiPredicate<Integer, ItemStack> filter;
    private final Runnable onChange;

    public ItemHandlerSimple(int size, BiPredicate<Integer, ItemStack> filter, Runnable onChange) {
        super(size);
        this.filter = filter;
        this.onChange = onChange;
    }

    public ItemHandlerSimple(int size, Runnable onChange) {
        this(size, (slot, stack) -> true, onChange);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return filter.test(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChange.run();
    }

    public void save(ValueOutput output, String key) {
        ContainerHelper.saveAllItems(output.child(key), getItems());
    }

    public void load(ValueInput input, String key) {
        clearContent();
        input.child(key).ifPresent(child -> ContainerHelper.loadAllItems(child, getItems()));
    }

    /** Inserts as much of the stack as possible into the given slot, ignoring the filter.
     * @return The leftover items. */
    public ItemStack forceInsert(int slot, ItemStack stack) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            setItem(slot, stack);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(current, stack)) {
            int space = current.getMaxStackSize() - current.getCount();
            int move = Math.min(space, stack.getCount());
            current.grow(move);
            setChanged();
            ItemStack left = stack.copy();
            left.shrink(move);
            return left;
        }
        return stack;
    }
}
