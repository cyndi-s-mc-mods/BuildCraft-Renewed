/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.tile.TileBC;
import buildcraft.lib.transfer.ISnapshotable;

/** A 16 bucket tank. Tanks stacked on top of each other act as one tank, filling from the bottom up. */
public class TileTank extends TileBC implements IFluidHandlerProvider {
    public static final int CAPACITY = 16 * BCFluidStack.BUCKET;
    private static final int SYNC_INTERVAL = 4;

    public final Tank tank = new Tank("tank", CAPACITY, this::onTankChanged);
    private final ColumnHandler handler = new ColumnHandler();
    private boolean needsSync = false;
    private long lastSync = 0;
    private int lastComparatorLevel = -1;
    /** Client side: the amount shown, moving smoothly towards the real amount. */
    private float renderAmount, lastRenderAmount;

    public TileTank(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.TANK_TILE.get(), pos, state);
    }

    private void onTankChanged() {
        setChanged();
        needsSync = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input);
    }

    @Override
    public void tick() {
        if (level == null) return;
        if (level.isClientSide()) {
            lastRenderAmount = renderAmount;
            float target = tank.getFluidAmount();
            renderAmount += (target - renderAmount) / 4;
            if (Math.abs(target - renderAmount) < 1) renderAmount = target;
        } else {
            if (needsSync && level.getGameTime() - lastSync >= SYNC_INTERVAL) {
                needsSync = false;
                lastSync = level.getGameTime();
                sendNetworkUpdate();
            }
            int comparator = getComparatorLevel();
            if (comparator != lastComparatorLevel) {
                lastComparatorLevel = comparator;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
        super.tick();
    }

    public float getFluidAmountForRender(float partialTicks) {
        return lastRenderAmount + (renderAmount - lastRenderAmount) * partialTicks;
    }

    public int getComparatorLevel() {
        int amount = tank.getFluidAmount();
        return amount * 14 / tank.getCapacity() + (amount > 0 ? 1 : 0);
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level != null && !level.isClientSide()) {
            balanceTankFluids();
        }
    }

    /** Moves fluid down the column so that it sits at the bottom. */
    public void balanceTankFluids() {
        List<TileTank> tanks = getTanks();
        BCFluidStack fluid = BCFluidStack.EMPTY;
        for (TileTank tile : tanks) {
            BCFluidStack held = tile.tank.getFluid();
            if (held.isEmpty()) continue;
            if (fluid.isEmpty()) {
                fluid = held;
            } else if (!fluid.isSameFluid(held)) {
                return;
            }
        }
        if (fluid.isEmpty()) return;
        for (int i = 0; i < tanks.size() - 1; i++) {
            TileTank lower = tanks.get(i);
            for (int j = i + 1; j < tanks.size(); j++) {
                Tank upper = tanks.get(j).tank;
                int space = lower.tank.getCapacity() - lower.tank.getFluidAmount();
                if (space <= 0) break;
                BCFluidStack moved = upper.drainInternal(space, false);
                lower.tank.fillInternal(moved, false);
            }
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (FluidUtilBC.interactWithHandler(player, hand, handler)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** @return Every tank in this column, from bottom to top. */
    private List<TileTank> getTanks() {
        Deque<TileTank> tanks = new ArrayDeque<>();
        tanks.add(this);
        if (level == null) return new ArrayList<>(tanks);
        BlockPos p = worldPosition.above();
        while (level.getBlockEntity(p) instanceof TileTank up) {
            tanks.addLast(up);
            p = p.above();
        }
        p = worldPosition.below();
        while (level.getBlockEntity(p) instanceof TileTank down) {
            tanks.addFirst(down);
            p = p.below();
        }
        return new ArrayList<>(tanks);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return handler;
    }

    /** The whole column of tanks seen as a single tank. */
    private final class ColumnHandler implements IFluidHandlerBC, ISnapshotable {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public BCFluidStack getFluidInTank(int index) {
            BCFluidStack total = BCFluidStack.EMPTY;
            int amount = 0;
            for (TileTank t : TileTank.this.getTanks()) {
                if (!t.tank.isEmpty()) {
                    total = t.tank.getFluid();
                    amount += t.tank.getFluidAmount();
                }
            }
            return total.withAmount(amount);
        }

        @Override
        public int getTankCapacity(int index) {
            return TileTank.this.getTanks().size() * CAPACITY;
        }

        @Override
        public boolean isFluidValid(int index, BCFluidStack stack) {
            BCFluidStack current = getFluidInTank(0);
            return current.isEmpty() || current.isSameFluid(stack);
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return 0;
            List<TileTank> tanks = TileTank.this.getTanks();
            for (TileTank t : tanks) {
                if (!t.tank.isEmpty() && !t.tank.getFluid().isSameFluid(resource)) {
                    return 0;
                }
            }
            if (isGas(resource)) {
                Collections.reverse(tanks);
            }
            int filled = 0;
            BCFluidStack left = resource;
            for (TileTank t : tanks) {
                int tankFilled = t.tank.fillInternal(left, simulate);
                filled += tankFilled;
                left = left.withAmount(left.getAmount() - tankFilled);
                if (left.isEmpty()) break;
            }
            return filled;
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            return drain(f -> f.isSameFluid(resource), resource.getAmount(), simulate);
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            if (maxDrain <= 0) return BCFluidStack.EMPTY;
            List<TileTank> tanks = TileTank.this.getTanks();
            BCFluidStack current = getFluidInTank(0);
            if (current.isEmpty() || !filter.test(current)) return BCFluidStack.EMPTY;
            if (!isGas(current)) {
                Collections.reverse(tanks);
            }
            int total = 0;
            for (TileTank t : tanks) {
                int realMax = maxDrain - total;
                if (realMax <= 0) break;
                total += t.tank.drainInternal(realMax, simulate).getAmount();
            }
            return current.withAmount(total);
        }

        private boolean isGas(BCFluidStack fluid) {
            return fluid.getFluid() instanceof buildcraft.lib.fluid.BCFluid bc && bc.def.isGaseous();
        }

        @Override
        public Object createSnapshot() {
            List<TileTank> tanks = TileTank.this.getTanks();
            Object[] snapshot = new Object[tanks.size() * 2];
            for (int i = 0; i < tanks.size(); i++) {
                snapshot[i * 2] = tanks.get(i);
                snapshot[i * 2 + 1] = tanks.get(i).tank.createSnapshot();
            }
            return snapshot;
        }

        @Override
        public void restoreSnapshot(Object snapshot) {
            Object[] array = (Object[]) snapshot;
            for (int i = 0; i < array.length; i += 2) {
                ((TileTank) array[i]).tank.restoreSnapshot(array[i + 1]);
            }
        }

        @Override
        public void onSnapshotCommit() {
            for (TileTank t : TileTank.this.getTanks()) {
                t.onTankChanged();
            }
        }
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) buildcraft.core.item.ItemFragileFluidShard.dropFluids(level, pos, tank);
    }
}
