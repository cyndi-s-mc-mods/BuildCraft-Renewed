/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.StackUtil;
import buildcraft.transport.container.ContainerDiamondWoodPipe;

/** Emerald (diamond-wood) pipes extract only the items in their filters, or everything else in blacklist mode. */
public class PipeBehaviourWoodDiamond extends PipeBehaviourWood {
    public enum FilterMode {
        WHITE_LIST,
        BLACK_LIST,
        ROUND_ROBIN;

        public static FilterMode get(int index) {
            return index >= 0 && index < values().length ? values()[index] : WHITE_LIST;
        }
    }

    public final ItemHandlerSimple filters = new ItemHandlerSimple(9, this::onFiltersChanged);
    public FilterMode filterMode = FilterMode.WHITE_LIST;
    public int currentFilter = 0;

    public PipeBehaviourWoodDiamond(IPipe pipe) {
        super(pipe);
    }

    private void onFiltersChanged() {
        if (filters.getItem(currentFilter).isEmpty()) {
            advanceFilter();
        }
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        filters.save(output, "filters");
        output.putInt("mode", filterMode.ordinal());
        output.putInt("currentFilter", currentFilter);
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        filters.load(input, "filters");
        filterMode = FilterMode.get(input.getIntOr("mode", 0));
        currentFilter = Math.floorMod(input.getIntOr("currentFilter", 0), filters.getContainerSize());
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (held.getItem() instanceof IToolWrench) {
            return super.onPipeActivate(player, hand, held, hit, part);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable MenuProvider getMenuProvider() {
        return new SimpleMenuProvider((id, inv, player) -> new ContainerDiamondWoodPipe(id, inv, this),
            Component.translatable(((BlockEntity) pipe.getHolder()).getBlockState().getBlock().getDescriptionId()));
    }

    private boolean hasAnyFilter() {
        return !filters.isEmpty();
    }

    private boolean matchesAnyFilter(ItemStack stack) {
        for (int i = 0; i < filters.getContainerSize(); i++) {
            if (StackUtil.isMatchingItemOrList(filters.getItem(i), stack)) return true;
        }
        return false;
    }

    private Predicate<ItemStack> getStackFilter() {
        return switch (filterMode) {
            case WHITE_LIST -> hasAnyFilter() ? this::matchesAnyFilter : s -> true;
            case BLACK_LIST -> s -> !matchesAnyFilter(s);
            case ROUND_ROBIN -> s -> StackUtil.isMatchingItemOrList(filters.getItem(currentFilter), s);
        };
    }

    @Override
    protected int extractItems(IFlowItems flow, Direction dir, int count, boolean simulate) {
        if (filters.getItem(currentFilter).isEmpty()) {
            advanceFilter();
        }
        int extracted = flow.tryExtractItems(1, dir, null, getStackFilter(), simulate);
        if (extracted > 0 && filterMode == FilterMode.ROUND_ROBIN && !simulate) {
            advanceFilter();
        }
        return extracted;
    }

    private boolean fluidMatches(BCFluidStack fluid) {
        for (int i = 0; i < filters.getContainerSize(); i++) {
            Fluid f = FluidUtilBC.getBucketFluid(filters.getItem(i));
            if (f != Fluids.EMPTY && fluid.isSameFluid(f)) return true;
        }
        return false;
    }

    @Override
    protected BCFluidStack extractFluid(IFlowFluid flow, Direction dir, int millibuckets, boolean simulate) {
        return switch (filterMode) {
            case WHITE_LIST -> hasAnyFilter() ? flow.tryExtractFluid(millibuckets, dir, this::fluidMatches, simulate)
                : flow.tryExtractFluid(millibuckets, dir, f -> true, simulate);
            case BLACK_LIST -> flow.tryExtractFluid(millibuckets, dir, f -> !fluidMatches(f), simulate);
            case ROUND_ROBIN -> BCFluidStack.EMPTY;
        };
    }

    public void setFilterMode(FilterMode mode) {
        if (filterMode != mode) {
            filterMode = mode;
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }

    private void advanceFilter() {
        int lastFilter = currentFilter;
        for (int i = 0; i < filters.getContainerSize(); i++) {
            currentFilter = (currentFilter + 1) % filters.getContainerSize();
            if (!filters.getItem(currentFilter).isEmpty()) {
                break;
            }
        }
        if (lastFilter != currentFilter && pipe.getHolder().getPipeWorld() != null
            && !pipe.getHolder().getPipeWorld().isClientSide()) {
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }
}
