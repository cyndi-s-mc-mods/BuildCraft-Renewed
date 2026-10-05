package buildcraft.lib.item;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;

/** Implemented by items that show several variants (such as every gate type) in the creative tab. */
public interface ICreativeVariants {
    void addCreativeVariants(Consumer<ItemStack> output);
}
