package buildcraft.lib.fluid;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.lib.transfer.ISnapshotable;

/** Several tanks seen from outside as one fluid handler. Fills go to the first tank that accepts the fluid. */
public class TankManager implements IFluidHandlerBC, ISnapshotable {
    private final List<Tank> tanks;

    public TankManager(Tank... tanks) {
        this.tanks = List.of(tanks);
    }

    public List<Tank> tanks() {
        return tanks;
    }

    public void save(ValueOutput output) {
        for (Tank tank : tanks) {
            tank.save(output);
        }
    }

    public void load(ValueInput input) {
        for (Tank tank : tanks) {
            tank.load(input);
        }
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public BCFluidStack getFluidInTank(int tank) {
        return tanks.get(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks.get(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, BCFluidStack stack) {
        return tanks.get(tank).isFluidValid(0, stack);
    }

    @Override
    public int fill(BCFluidStack resource, boolean simulate) {
        // Prefer a tank that already has this fluid
        for (Tank tank : tanks) {
            if (!tank.isEmpty() && tank.getFluid().isSameFluid(resource)) {
                int filled = tank.fill(resource, simulate);
                if (filled > 0) return filled;
            }
        }
        for (Tank tank : tanks) {
            int filled = tank.fill(resource, simulate);
            if (filled > 0) return filled;
        }
        return 0;
    }

    @Override
    public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
        for (Tank tank : tanks) {
            BCFluidStack drained = tank.drain(resource, simulate);
            if (!drained.isEmpty()) return drained;
        }
        return BCFluidStack.EMPTY;
    }

    @Override
    public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
        for (Tank tank : tanks) {
            BCFluidStack drained = tank.drain(filter, maxDrain, simulate);
            if (!drained.isEmpty()) return drained;
        }
        return BCFluidStack.EMPTY;
    }

    @Override
    public Object createSnapshot() {
        Object[] snapshots = new Object[tanks.size()];
        for (int i = 0; i < snapshots.length; i++) {
            snapshots[i] = tanks.get(i).createSnapshot();
        }
        return snapshots;
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        Object[] snapshots = (Object[]) snapshot;
        for (int i = 0; i < snapshots.length; i++) {
            tanks.get(i).restoreSnapshot(snapshots[i]);
        }
    }

    @Override
    public void onSnapshotCommit() {
        for (Tank tank : tanks) {
            tank.onSnapshotCommit();
        }
    }
}
