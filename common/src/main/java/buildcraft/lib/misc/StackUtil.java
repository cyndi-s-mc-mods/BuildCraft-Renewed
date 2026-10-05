package buildcraft.lib.misc;

import net.minecraft.world.item.ItemStack;

public final class StackUtil {
    private StackUtil() {}

    /** @return True if the stack matches the filter: same item and components, ignoring the count. An empty filter
     *         matches nothing. */
    public static boolean isMatchingItemOrList(ItemStack filter, ItemStack stack) {
        if (filter.isEmpty() || stack.isEmpty()) return false;
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
