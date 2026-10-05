package buildcraft.lib.fluid;

import java.util.function.Predicate;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.lib.transfer.ISnapshotable;

/** A single fluid tank. */
public class Tank implements IFluidHandlerBC, ISnapshotable {
    public final String name;
    private final int capacity;
    private final Predicate<BCFluidStack> filter;
    private final Runnable onChange;
    private BCFluidStack fluid = BCFluidStack.EMPTY;
    public boolean canFill = true;
    public boolean canDrain = true;

    public Tank(String name, int capacity, Predicate<BCFluidStack> filter, Runnable onChange) {
        this.name = name;
        this.capacity = capacity;
        this.filter = filter;
        this.onChange = onChange;
    }

    public Tank(String name, int capacity, Runnable onChange) {
        this(name, capacity, f -> true, onChange);
    }

    public BCFluidStack getFluid() {
        return fluid;
    }

    public int getFluidAmount() {
        return fluid.getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public boolean isFull() {
        return fluid.getAmount() >= capacity;
    }

    public void setFluid(BCFluidStack fluid) {
        this.fluid = fluid;
        onChange.run();
    }

    public void save(ValueOutput output) {
        fluid.save(output.child(name));
    }

    public void load(ValueInput input) {
        fluid = input.child(name).map(BCFluidStack::load).orElse(BCFluidStack.EMPTY);
    }

    /** Fills ignoring {@link #canFill}, for the machine's own use. */
    public int fillInternal(BCFluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !filter.test(resource)) return 0;
        if (!fluid.isEmpty() && !fluid.isSameFluid(resource)) return 0;
        int toFill = Math.min(capacity - fluid.getAmount(), resource.getAmount());
        if (toFill <= 0) return 0;
        if (!simulate) {
            fluid = resource.withAmount(fluid.getAmount() + toFill);
            onChange.run();
        }
        return toFill;
    }

    /** Drains ignoring {@link #canDrain}, for the machine's own use. */
    public BCFluidStack drainInternal(int maxDrain, boolean simulate) {
        if (fluid.isEmpty() || maxDrain <= 0) return BCFluidStack.EMPTY;
        int drained = Math.min(maxDrain, fluid.getAmount());
        BCFluidStack result = fluid.withAmount(drained);
        if (!simulate) {
            fluid = fluid.withAmount(fluid.getAmount() - drained);
            onChange.run();
        }
        return result;
    }

    // IFluidHandlerBC

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public BCFluidStack getFluidInTank(int tank) {
        return fluid;
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, BCFluidStack stack) {
        return filter.test(stack);
    }

    @Override
    public int fill(BCFluidStack resource, boolean simulate) {
        return canFill ? fillInternal(resource, simulate) : 0;
    }

    @Override
    public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
        if (!canDrain || resource.isEmpty() || !fluid.isSameFluid(resource)) return BCFluidStack.EMPTY;
        return drainInternal(resource.getAmount(), simulate);
    }

    @Override
    public BCFluidStack drain(Predicate<BCFluidStack> drainFilter, int maxDrain, boolean simulate) {
        if (!canDrain || fluid.isEmpty() || !drainFilter.test(fluid)) return BCFluidStack.EMPTY;
        return drainInternal(maxDrain, simulate);
    }

    // ISnapshotable

    @Override
    public Object createSnapshot() {
        return fluid;
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        fluid = (BCFluidStack) snapshot;
    }

    @Override
    public void onSnapshotCommit() {
        onChange.run();
    }
}
