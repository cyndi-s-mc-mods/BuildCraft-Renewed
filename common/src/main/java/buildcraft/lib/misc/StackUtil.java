package buildcraft.lib.misc;

import net.minecraft.world.item.ItemStack;

import buildcraft.lib.BCLibComponents;
import buildcraft.lib.list.ListData;

public final class StackUtil {
    private StackUtil() {}

    /** @return True if the stack matches the filter: same item and components, ignoring the count, or (if the filter is
     *         a list) anything the list matches. An empty filter matches nothing. */
    public static boolean isMatchingItemOrList(ItemStack filter, ItemStack stack) {
        if (filter.isEmpty() || stack.isEmpty()) return false;
        ListData list = filter.get(BCLibComponents.LIST.get());
        if (list != null) return list.matches(stack);
        return ItemStack.isSameItemSameComponents(filter, stack);
    }

    public static int findHighestCommonFactor(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}
