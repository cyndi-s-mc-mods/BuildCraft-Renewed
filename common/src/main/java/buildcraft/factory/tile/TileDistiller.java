/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.api.recipes.RefineryRecipes;
import buildcraft.api.tiles.IHasWork;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.tile.TileBC;

/** Splits oils and fuels into a lighter gas, which leaves from the top, and a denser liquid, which leaves from the
 * bottom. The input goes in from the sides. */
public class TileDistiller extends TileBC implements IFluidHandlerProvider, IMjConnectorProvider, MenuProvider, IHasWork {
    public static final long MAX_MJ_PER_TICK = 6 * MjAPI.MJ;
    public static final int CAPACITY = 4 * BCFluidStack.BUCKET;
    private static final int SYNC_INTERVAL = 4;

    public final Tank tankIn = new Tank("in", CAPACITY, f -> RefineryRecipes.getDistillation(f) != null, this::onTankChanged);
    public final Tank tankGasOut = new Tank("gasOut", CAPACITY, this::onTankChanged);
    public final Tank tankLiquidOut = new Tank("liquidOut", CAPACITY, this::onTankChanged);
    private final MjBattery battery = new MjBattery(1024 * MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);

    private long distillPower = 0;
    /** Average power used over the last few seconds, for the GUI. */
    private long powerAverage = 0;
    public boolean isActive = false;
    private boolean needsSync = false;
    private long lastSync = 0;

    public TileDistiller(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.DISTILLER_TILE.get(), pos, state);
        tankIn.canDrain = false;
        tankGasOut.canFill = false;
        tankLiquidOut.canFill = false;
    }

    private void onTankChanged() {
        setChanged();
        needsSync = true;
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            serverTick();
            if (needsSync && level.getGameTime() - lastSync >= SYNC_INTERVAL) {
                needsSync = false;
                lastSync = level.getGameTime();
                sendNetworkUpdate();
            }
        }
        super.tick();
    }

    private void serverTick() {
        battery.tick();
        RefineryRecipes.Distillation recipe = RefineryRecipes.getDistillation(tankIn.getFluid());
        boolean wasActive = isActive;
        long used = 0;
        if (recipe != null && tankIn.getFluidAmount() >= recipe.in().getAmount()
            && tankGasOut.fillInternal(recipe.outGas(), true) == recipe.outGas().getAmount()
            && tankLiquidOut.fillInternal(recipe.outLiquid(), true) == recipe.outLiquid().getAmount()) {
            // Work faster when the battery is fuller
            long max = MAX_MJ_PER_TICK;
            max *= battery.getStored() + max;
            max /= battery.getCapacity() / 2;
            max = Math.min(max, MAX_MJ_PER_TICK);
            used = battery.extractPower(0, max);
            distillPower += used;
            isActive = used > 0;
            if (distillPower >= recipe.powerRequired()) {
                distillPower -= recipe.powerRequired();
                tankIn.drainInternal(recipe.in().getAmount(), false);
                tankGasOut.fillInternal(recipe.outGas(), false);
                tankLiquidOut.fillInternal(recipe.outLiquid(), false);
            }
        } else {
            battery.addPowerChecking(distillPower, false);
            distillPower = 0;
            isActive = false;
        }
        powerAverage += (used - powerAverage) / 20;
        if (wasActive != isActive) {
            needsSync = true;
        }
    }

    /** @return The average power used each tick, from 0 to 1 of {@link #MAX_MJ_PER_TICK}. */
    public double getPowerFraction() {
        return Math.min(1, powerAverage / (double) MAX_MJ_PER_TICK);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        if (side == Direction.UP) return tankGasOut;
        if (side == Direction.DOWN) return tankLiquidOut;
        return tankIn;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.isEmpty() && (FluidUtilBC.interactWithHandler(player, hand, tankIn)
            || FluidUtilBC.interactWithHandler(player, hand, tankLiquidOut)
            || FluidUtilBC.interactWithHandler(player, hand, tankGasOut))) {
            return InteractionResult.SUCCESS;
        }
        if (!player.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tankIn.save(output);
        tankGasOut.save(output);
        tankLiquidOut.save(output);
        battery.save(output, "battery");
        output.putLong("distillPower", distillPower);
        output.putBoolean("active", isActive);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tankIn.load(input);
        tankGasOut.load(input);
        tankLiquidOut.load(input);
        battery.load(input, "battery");
        distillPower = input.getLongOr("distillPower", 0);
        isActive = input.getBooleanOr("active", false);
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerDistiller(id, inventory, this);
    }

    @Override
    public boolean hasWork() {
        return isActive;
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) buildcraft.core.item.ItemFragileFluidShard.dropFluids(level, pos, tankIn, tankGasOut, tankLiquidOut);
    }
}
