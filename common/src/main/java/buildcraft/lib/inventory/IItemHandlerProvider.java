package buildcraft.lib.inventory;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/** Implemented by BuildCraft block entities that take or give items without being a vanilla Container (such as
 * pipes). Block entities that are Containers are exposed to other mods through the loader's own wrappers. */
public interface IItemHandlerProvider {
    @Nullable
    IItemTransactor getItemTransactor(@Nullable Direction side);
}
