package buildcraft.lib.fluid;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

/** Implemented by BuildCraft block entities with tanks. Each loader exposes these tanks to other mods. */
public interface IFluidHandlerProvider {
    /** @param side The side being accessed, or null for internal access. */
    @Nullable
    IFluidHandlerBC getFluidHandler(@Nullable Direction side);
}
