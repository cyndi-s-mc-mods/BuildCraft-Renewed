package buildcraft.lib.fluid;

import java.util.function.Predicate;

/** Something that holds fluids in one or more tanks. Amounts are in millibuckets. Used both for BuildCraft's own
 * tanks and (through the platform) for tanks from other mods. */
public interface IFluidHandlerBC {
    int getTanks();

    BCFluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, BCFluidStack stack);

    /** @return How much was (or would be) filled. */
    int fill(BCFluidStack resource, boolean simulate);

    /** Drains up to resource's amount of resource's fluid. */
    BCFluidStack drain(BCFluidStack resource, boolean simulate);

    /** Drains up to maxDrain of whatever fluids the filter accepts. */
    BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate);

    default BCFluidStack drain(int maxDrain, boolean simulate) {
        return drain(f -> true, maxDrain, simulate);
    }
}
