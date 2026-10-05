package buildcraft.forge;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import buildcraft.BuildCraft;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.inventory.IItemHandlerProvider;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.registry.RegistrationHelper;

/** Connects BuildCraft's inventories and tanks to Forge's capabilities, in both directions. */
final class ForgeTransfer {
    private ForgeTransfer() {}

    private static IFluidHandler.FluidAction action(boolean simulate) {
        return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
    }

    private static FluidStack toForge(BCFluidStack stack) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), stack.getAmount());
    }

    private static BCFluidStack fromForge(FluidStack stack) {
        return stack.isEmpty() ? BCFluidStack.EMPTY : BCFluidStack.of(stack.getFluid(), stack.getAmount());
    }

    // ------------------------------------------------------------------ Using other mods' storage

    static @Nullable IItemTransactor getItemTransactor(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
        if (handler == null) {
            if (be instanceof WorldlyContainer worldly) handler = new SidedInvWrapper(worldly, side);
            else if (be instanceof Container container) handler = new InvWrapper(container);
            else return null;
        }
        return new ItemTransactorWrapper(handler);
    }

    static @Nullable IFluidHandlerBC getFluidHandler(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        IFluidHandler handler = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve().orElse(null);
        return handler == null ? null : new FluidHandlerWrapper(handler);
    }

    private record ItemTransactorWrapper(IItemHandler handler) implements IItemTransactor {
        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), simulate);
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int min, int max, boolean simulate) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty() || !filter.test(inSlot)) continue;
                ItemStack extracted = handler.extractItem(slot, max, true);
                if (extracted.getCount() < min || extracted.isEmpty()) continue;
                return simulate ? extracted : handler.extractItem(slot, max, false);
            }
            return ItemStack.EMPTY;
        }
    }

    private record FluidHandlerWrapper(IFluidHandler handler) implements IFluidHandlerBC {
        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public BCFluidStack getFluidInTank(int tank) {
            return fromForge(handler.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, BCFluidStack stack) {
            return handler.isFluidValid(tank, toForge(stack));
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            return handler.fill(toForge(resource), action(simulate));
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            return fromForge(handler.drain(toForge(resource), action(simulate)));
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            for (int i = 0; i < handler.getTanks(); i++) {
                BCFluidStack inTank = getFluidInTank(i);
                if (inTank.isEmpty() || !filter.test(inTank)) continue;
                BCFluidStack drained = drain(inTank.withAmount(maxDrain), simulate);
                if (!drained.isEmpty()) return drained;
            }
            return BCFluidStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------ Exposing BuildCraft's storage

    private static Set<BlockEntityType<?>> types;

    static void register() {
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(event -> {
            if (types == null) {
                types = new HashSet<>(RegistrationHelper.tileTypes());
            }
            BlockEntity be = event.getObject();
            if (types.contains(be.getType())) {
                event.addCapability(BuildCraft.id("transfer"), new Provider(be));
            }
        });
    }

    private record Provider(BlockEntity be) implements ICapabilityProvider {
        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                if (be instanceof IItemHandlerProvider provider) {
                    IItemTransactor transactor = provider.getItemTransactor(side);
                    if (transactor != null) return LazyOptional.of(() -> new ItemInsertHandler(transactor)).cast();
                } else if (be instanceof WorldlyContainer worldly) {
                    return LazyOptional.of(() -> new SidedInvWrapper(worldly, side)).cast();
                } else if (be instanceof Container container) {
                    return LazyOptional.of(() -> new InvWrapper(container)).cast();
                }
            } else if (cap == ForgeCapabilities.FLUID_HANDLER && be instanceof IFluidHandlerProvider provider) {
                IFluidHandlerBC handler = provider.getFluidHandler(side);
                if (handler != null) return LazyOptional.of(() -> new FluidHandlerAdapter(handler)).cast();
            }
            return LazyOptional.empty();
        }
    }

    /** Lets other mods insert into things like pipes. */
    private record ItemInsertHandler(IItemTransactor transactor) implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return transactor.insert(stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }

    private record FluidHandlerAdapter(IFluidHandlerBC handler) implements IFluidHandler {
        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return toForge(handler.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return handler.isFluidValid(tank, fromForge(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return handler.fill(fromForge(resource), action.simulate());
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return toForge(handler.drain(fromForge(resource), action.simulate()));
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return toForge(handler.drain(maxDrain, action.simulate()));
        }
    }
}
