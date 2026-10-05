package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.platform.Platform;
import buildcraft.transport.tile.TilePipeHolder;

public final class InventoryUtil {
    private InventoryUtil() {}

    /** Puts the stack into an adjacent pipe (preferred) or inventory, or drops it if nothing takes it. */
    public static void addToBestAcceptor(Level level, BlockPos pos, ItemStack stack) {
        stack = addToAdjacent(level, pos, stack);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, stack);
        }
    }

    /** @return What couldn't be inserted. */
    public static ItemStack addToAdjacent(Level level, BlockPos pos, ItemStack stack) {
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(sides);
        // Pipes first, as they are usually how players expect items to leave a machine
        sides.sort((a, b) -> Boolean.compare(!isPipe(level, pos.relative(a)), !isPipe(level, pos.relative(b))));
        for (Direction side : sides) {
            if (stack.isEmpty()) break;
            IItemTransactor transactor = Platform.INSTANCE.getItemTransactor(level, pos.relative(side), side.getOpposite());
            if (transactor != null) {
                stack = transactor.insert(stack, false);
            }
        }
        return stack;
    }

    private static boolean isPipe(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TilePipeHolder;
    }
}
