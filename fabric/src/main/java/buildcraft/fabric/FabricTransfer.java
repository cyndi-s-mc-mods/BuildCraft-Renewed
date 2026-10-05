package buildcraft.fabric;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.inventory.IItemHandlerProvider;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.transfer.ISnapshotable;

/** Connects BuildCraft's inventories and tanks to Fabric's transfer API, in both directions. */
final class FabricTransfer {
    /** Fabric measures fluids in droplets, 81 per millibucket. */
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / BCFluidStack.BUCKET;

    private FabricTransfer() {}

    private static Transaction open() {
        return Transaction.openNested(Transaction.isOpen() ? Transaction.getCurrentUnsafe() : null);
    }

    // ------------------------------------------------------------------ Using other mods' storage

    static @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new ItemTransactorWrapper(storage);
    }

    static @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FluidHandlerWrapper(storage);
    }

    private record ItemTransactorWrapper(Storage<ItemVariant> storage) implements IItemTransactor {
        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = open()) {
                long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                return stack.copyWithCount(stack.getCount() - (int) inserted);
            }
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int min, int max, boolean simulate) {
            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                ItemVariant variant = view.getResource();
                ItemStack template = variant.toStack();
                if (!filter.test(template)) continue;
                try (Transaction tx = open()) {
                    long extracted = storage.extract(variant, Math.min(max, template.getMaxStackSize()), tx);
                    if (extracted < min || extracted == 0) continue;
                    if (!simulate) tx.commit();
                    return variant.toStack((int) extracted);
                }
            }
            return ItemStack.EMPTY;
        }
    }

    private record FluidHandlerWrapper(Storage<FluidVariant> storage) implements IFluidHandlerBC {
        private List<StorageView<FluidVariant>> views() {
            List<StorageView<FluidVariant>> list = new ArrayList<>();
            Iterator<StorageView<FluidVariant>> it = storage.iterator();
            while (it.hasNext()) list.add(it.next());
            return list;
        }

        @Override
        public int getTanks() {
            return views().size();
        }

        @Override
        public BCFluidStack getFluidInTank(int tank) {
            List<StorageView<FluidVariant>> views = views();
            if (tank >= views.size()) return BCFluidStack.EMPTY;
            StorageView<FluidVariant> view = views.get(tank);
            if (view.isResourceBlank()) return BCFluidStack.EMPTY;
            return BCFluidStack.of(view.getResource().getFluid(), (int) (view.getAmount() / DROPLETS_PER_MB));
        }

        @Override
        public int getTankCapacity(int tank) {
            List<StorageView<FluidVariant>> views = views();
            return tank >= views.size() ? 0 : (int) (views.get(tank).getCapacity() / DROPLETS_PER_MB);
        }

        @Override
        public boolean isFluidValid(int tank, BCFluidStack stack) {
            return true;
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return 0;
            FluidVariant variant = FluidVariant.of(resource.getFluid());
            try (Transaction tx = open()) {
                long inserted = storage.insert(variant, resource.getAmount() * DROPLETS_PER_MB, tx);
                int mb = (int) (inserted / DROPLETS_PER_MB);
                if (mb * DROPLETS_PER_MB != inserted) {
                    // Only move whole millibuckets
                    tx.abort();
                    try (Transaction retry = open()) {
                        storage.insert(variant, mb * DROPLETS_PER_MB, retry);
                        if (!simulate) retry.commit();
                    }
                    return mb;
                }
                if (!simulate) tx.commit();
                return mb;
            }
        }

        private BCFluidStack extract(FluidVariant variant, int maxMb, boolean simulate) {
            if (maxMb <= 0) return BCFluidStack.EMPTY;
            try (Transaction tx = open()) {
                long extracted = storage.extract(variant, maxMb * DROPLETS_PER_MB, tx);
                int mb = (int) (extracted / DROPLETS_PER_MB);
                if (mb * DROPLETS_PER_MB != extracted) {
                    tx.abort();
                    try (Transaction retry = open()) {
                        storage.extract(variant, mb * DROPLETS_PER_MB, retry);
                        if (!simulate) retry.commit();
                    }
                } else if (!simulate) {
                    tx.commit();
                }
                return BCFluidStack.of(variant.getFluid(), mb);
            }
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return BCFluidStack.EMPTY;
            return extract(FluidVariant.of(resource.getFluid()), resource.getAmount(), simulate);
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                FluidVariant variant = view.getResource();
                BCFluidStack stack = BCFluidStack.of(variant.getFluid(), (int) (view.getAmount() / DROPLETS_PER_MB));
                if (stack.isEmpty() || !filter.test(stack)) continue;
                BCFluidStack drained = extract(variant, maxDrain, simulate);
                if (!drained.isEmpty()) return drained;
            }
            return BCFluidStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------ Exposing BuildCraft's storage

    static void register() {
        BlockEntityType<?>[] types = RegistrationHelper.tileTypes().toArray(new BlockEntityType<?>[0]);
        ItemStorage.SIDED.registerForBlockEntities((be, side) -> {
            if (be instanceof IItemHandlerProvider provider) {
                IItemTransactor transactor = provider.getItemTransactor(side);
                return transactor == null ? null : new ItemInsertStorage(transactor);
            }
            // Containers fall through to Fabric's own container wrapper
            return null;
        }, types);
        FluidStorage.SIDED.registerForBlockEntities((be, side) -> {
            if (be instanceof IFluidHandlerProvider provider) {
                IFluidHandlerBC handler = provider.getFluidHandler(side);
                if (handler instanceof ISnapshotable) {
                    return new FluidStorageAdapter(handler);
                }
            }
            return null;
        }, types);
    }

    /** Lets other mods insert into things like pipes. The insert happens when the transaction is committed. */
    private static final class ItemInsertStorage extends SnapshotParticipant<List<ItemStack>> implements InsertionOnlyStorage<ItemVariant> {
        private final IItemTransactor transactor;
        private List<ItemStack> pending = new ArrayList<>();

        ItemInsertStorage(IItemTransactor transactor) {
            this.transactor = transactor;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            int count = (int) Math.min(maxAmount, resource.toStack().getMaxStackSize());
            ItemStack stack = resource.toStack(count);
            ItemStack leftover = transactor.insert(stack, true);
            int accepted = count - leftover.getCount();
            if (accepted > 0) {
                updateSnapshots(transaction);
                pending.add(resource.toStack(accepted));
            }
            return accepted;
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            return new ArrayList<>(pending);
        }

        @Override
        protected void readSnapshot(List<ItemStack> snapshot) {
            pending = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            for (ItemStack stack : pending) {
                transactor.insert(stack, false);
            }
            pending = new ArrayList<>();
        }
    }

    /** Exposes a BuildCraft tank (or set of tanks) as a Fabric fluid storage. */
    private static final class FluidStorageAdapter extends SnapshotParticipant<Object> implements Storage<FluidVariant> {
        private final IFluidHandlerBC handler;
        private final ISnapshotable snapshotable;

        FluidStorageAdapter(IFluidHandlerBC handler) {
            this.handler = handler;
            this.snapshotable = (ISnapshotable) handler;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || !resource.getComponents().isEmpty()) return 0;
            int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
            BCFluidStack stack = BCFluidStack.of(resource.getFluid(), mb);
            if (stack.isEmpty() || handler.fill(stack, true) <= 0) return 0;
            updateSnapshots(transaction);
            return handler.fill(stack, false) * DROPLETS_PER_MB;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || !resource.getComponents().isEmpty()) return 0;
            int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / DROPLETS_PER_MB);
            BCFluidStack stack = BCFluidStack.of(resource.getFluid(), mb);
            if (stack.isEmpty() || handler.drain(stack, true).isEmpty()) return 0;
            updateSnapshots(transaction);
            return handler.drain(stack, false).getAmount() * DROPLETS_PER_MB;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (int i = 0; i < handler.getTanks(); i++) {
                int tank = i;
                views.add(new StorageView<>() {
                    @Override
                    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                        BCFluidStack current = handler.getFluidInTank(tank);
                        if (current.isEmpty() || !current.isSameFluid(resource.getFluid())) return 0;
                        return FluidStorageAdapter.this.extract(resource, maxAmount, transaction);
                    }

                    @Override
                    public boolean isResourceBlank() {
                        return handler.getFluidInTank(tank).isEmpty();
                    }

                    @Override
                    public FluidVariant getResource() {
                        BCFluidStack current = handler.getFluidInTank(tank);
                        return current.isEmpty() ? FluidVariant.blank() : FluidVariant.of(current.getFluid());
                    }

                    @Override
                    public long getAmount() {
                        return handler.getFluidInTank(tank).getAmount() * DROPLETS_PER_MB;
                    }

                    @Override
                    public long getCapacity() {
                        return handler.getTankCapacity(tank) * DROPLETS_PER_MB;
                    }
                });
            }
            return views.iterator();
        }

        @Override
        protected Object createSnapshot() {
            return snapshotable.createSnapshot();
        }

        @Override
        protected void readSnapshot(Object snapshot) {
            snapshotable.restoreSnapshot(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            snapshotable.onSnapshotCommit();
        }
    }
}
