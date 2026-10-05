package buildcraft.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.inventory.IItemHandlerProvider;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.transfer.ISnapshotable;

/** Connects BuildCraft's inventories and tanks to NeoForge's transfer API, in both directions. */
final class NeoForgeTransfer {
    private NeoForgeTransfer() {}

    @SuppressWarnings("deprecation")
    private static Transaction open() {
        return Transaction.getLifecycle() == Transaction.Lifecycle.OPEN
            ? Transaction.open(Transaction.getCurrentOpenedTransaction())
            : Transaction.openRoot();
    }

    // ------------------------------------------------------------------ Using other mods' storage

    static @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler == null ? null : new ItemTransactorWrapper(handler);
    }

    static @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : new FluidHandlerWrapper(handler);
    }

    private record ItemTransactorWrapper(ResourceHandler<ItemResource> handler) implements IItemTransactor {
        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = open()) {
                int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                return stack.copyWithCount(stack.getCount() - inserted);
            }
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int min, int max, boolean simulate) {
            for (int i = 0; i < handler.size(); i++) {
                ItemResource resource = handler.getResource(i);
                if (resource.isEmpty() || handler.getAmountAsLong(i) <= 0) continue;
                ItemStack template = resource.toStack();
                if (!filter.test(template)) continue;
                try (Transaction tx = open()) {
                    int extracted = handler.extract(resource, Math.min(max, template.getMaxStackSize()), tx);
                    if (extracted < min || extracted == 0) continue;
                    if (!simulate) tx.commit();
                    return resource.toStack(extracted);
                }
            }
            return ItemStack.EMPTY;
        }
    }

    private record FluidHandlerWrapper(ResourceHandler<FluidResource> handler) implements IFluidHandlerBC {
        @Override
        public int getTanks() {
            return handler.size();
        }

        @Override
        public BCFluidStack getFluidInTank(int tank) {
            FluidResource resource = handler.getResource(tank);
            return resource.isEmpty() ? BCFluidStack.EMPTY : BCFluidStack.of(resource.getFluid(), handler.getAmountAsInt(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return handler.getCapacityAsInt(tank, handler.getResource(tank));
        }

        @Override
        public boolean isFluidValid(int tank, BCFluidStack stack) {
            return !stack.isEmpty() && handler.isValid(tank, FluidResource.of(stack.getFluid()));
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return 0;
            try (Transaction tx = open()) {
                int filled = handler.insert(FluidResource.of(resource.getFluid()), resource.getAmount(), tx);
                if (!simulate) tx.commit();
                return filled;
            }
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return BCFluidStack.EMPTY;
            try (Transaction tx = open()) {
                int drained = handler.extract(FluidResource.of(resource.getFluid()), resource.getAmount(), tx);
                if (!simulate) tx.commit();
                return resource.withAmount(drained);
            }
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            for (int i = 0; i < handler.size(); i++) {
                BCFluidStack inTank = getFluidInTank(i);
                if (inTank.isEmpty() || !filter.test(inTank)) continue;
                BCFluidStack drained = drain(inTank.withAmount(maxDrain), simulate);
                if (!drained.isEmpty()) return drained;
            }
            return BCFluidStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------ Exposing BuildCraft's storage

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<?> type : RegistrationHelper.tileTypes()) {
            register(event, type);
        }
    }

    private static <T extends BlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
            if (be instanceof IItemHandlerProvider provider) {
                IItemTransactor transactor = provider.getItemTransactor(side);
                return transactor == null ? null : new ItemInsertHandler(transactor);
            }
            if (be instanceof WorldlyContainer worldly && side != null) {
                return new WorldlyContainerWrapper(worldly, side);
            }
            if (be instanceof Container container) {
                return VanillaContainerWrapper.of(container);
            }
            return null;
        });
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> {
            if (be instanceof IFluidHandlerProvider provider) {
                IFluidHandlerBC handler = provider.getFluidHandler(side);
                if (handler instanceof ISnapshotable) {
                    return new FluidHandlerAdapter(handler);
                }
            }
            return null;
        });
    }

    /** Lets other mods insert into things like pipes. The insert happens when the root transaction is committed. */
    private static final class ItemInsertHandler extends SnapshotJournal<List<ItemStack>> implements ResourceHandler<ItemResource> {
        private final IItemTransactor transactor;
        private List<ItemStack> pending = new ArrayList<>();

        ItemInsertHandler(IItemTransactor transactor) {
            this.transactor = transactor;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return resource.isEmpty() ? 64 : resource.toStack().getMaxStackSize();
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return true;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            int count = Math.min(amount, resource.toStack().getMaxStackSize());
            ItemStack leftover = transactor.insert(resource.toStack(count), true);
            int accepted = count - leftover.getCount();
            if (accepted > 0) {
                updateSnapshots(transaction);
                pending.add(resource.toStack(accepted));
            }
            return accepted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            return new ArrayList<>(pending);
        }

        @Override
        protected void revertToSnapshot(List<ItemStack> snapshot) {
            pending = snapshot;
        }

        @Override
        protected void onRootCommit(List<ItemStack> originalState) {
            for (ItemStack stack : pending) {
                transactor.insert(stack, false);
            }
            pending = new ArrayList<>();
        }
    }

    /** Exposes a BuildCraft tank (or set of tanks) as a NeoForge fluid handler. */
    private static final class FluidHandlerAdapter extends SnapshotJournal<Object> implements ResourceHandler<FluidResource> {
        private final IFluidHandlerBC handler;
        private final ISnapshotable snapshotable;

        FluidHandlerAdapter(IFluidHandlerBC handler) {
            this.handler = handler;
            this.snapshotable = (ISnapshotable) handler;
        }

        @Override
        public int size() {
            return handler.getTanks();
        }

        @Override
        public FluidResource getResource(int index) {
            BCFluidStack stack = handler.getFluidInTank(index);
            return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack.getFluid());
        }

        @Override
        public long getAmountAsLong(int index) {
            return handler.getFluidInTank(index).getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return handler.getTankCapacity(index);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return !resource.isEmpty() && handler.isFluidValid(index, BCFluidStack.of(resource.getFluid(), 1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || !resource.isComponentsPatchEmpty() || amount <= 0) return 0;
            BCFluidStack stack = BCFluidStack.of(resource.getFluid(), amount);
            if (handler.fill(stack, true) <= 0) return 0;
            updateSnapshots(transaction);
            return handler.fill(stack, false);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || !resource.isComponentsPatchEmpty() || amount <= 0) return 0;
            BCFluidStack inTank = handler.getFluidInTank(index);
            if (inTank.isEmpty() || !inTank.isSameFluid(resource.getFluid())) return 0;
            BCFluidStack stack = BCFluidStack.of(resource.getFluid(), amount);
            if (handler.drain(stack, true).isEmpty()) return 0;
            updateSnapshots(transaction);
            return handler.drain(stack, false).getAmount();
        }

        @Override
        protected Object createSnapshot() {
            return snapshotable.createSnapshot();
        }

        @Override
        protected void revertToSnapshot(Object snapshot) {
            snapshotable.restoreSnapshot(snapshot);
        }

        @Override
        protected void onRootCommit(Object originalState) {
            snapshotable.onSnapshotCommit();
        }
    }
}
