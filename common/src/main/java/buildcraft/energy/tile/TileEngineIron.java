/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tools.IToolWrench;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.container.ContainerEngineIron;
import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.fluid.TankManager;
import buildcraft.lib.transfer.ISnapshotable;

/** The combustion engine: burns liquid fuels, and needs coolant to keep from overheating. */
public class TileEngineIron extends TileEngineBase implements IFluidHandlerProvider, MenuProvider {
    public static final int MAX_FLUID = 10_000;
    public static final double COOLDOWN_RATE = 0.05;
    public static final int MAX_COOLANT_PER_TICK = 40;

    public final Tank tankFuel = new Tank("fuel", MAX_FLUID, f -> BuildcraftFuelRegistry.getFuel(f) != null, this::setChanged);
    public final Tank tankCoolant = new Tank("coolant", MAX_FLUID, f -> BuildcraftFuelRegistry.getCoolant(f) != null,
        this::setChanged);
    public final Tank tankResidue = new Tank("residue", MAX_FLUID, this::isResidue, this::setChanged);
    private final TankManager tankManager = new TankManager(tankFuel, tankCoolant, tankResidue);
    private final IFluidHandlerBC fluidHandler = new ExternalFluidHandler();

    private int penaltyCooling = 0;
    private boolean lastPowered = false;
    private double burnTime;
    private double residueAmount = 0;
    private BuildcraftFuelRegistry.@Nullable Fuel currentFuel;

    public TileEngineIron(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_COMBUSTION_TILE.get(), pos, state);
        tankResidue.canFill = false;
    }

    private boolean isResidue(BCFluidStack fluid) {
        // The client doesn't know the current fuel, so trust the server
        if (level != null && level.isClientSide()) {
            return true;
        }
        return currentFuel != null && currentFuel.isDirty() && fluid.isSameFluid(currentFuel.residue());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tankManager.save(output);
        output.putInt("penaltyCooling", penaltyCooling);
        output.putDouble("burnTime", burnTime);
        output.putDouble("residueAmount", residueAmount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tankManager.load(input);
        penaltyCooling = input.getIntOr("penaltyCooling", 0);
        burnTime = input.getDoubleOr("burnTime", 0);
        residueAmount = Math.max(0, input.getDoubleOr("residueAmount", 0));
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.isEmpty() && held.getItem() instanceof IToolWrench) {
            return InteractionResult.PASS;
        }
        BuildcraftFuelRegistry.SolidCoolant solid = held.isEmpty() ? null : BuildcraftFuelRegistry.getSolidCoolant(held);
        if (solid != null) {
            if (tankCoolant.fillInternal(solid.fluid(), true) == solid.fluid().getAmount()) {
                if (!isClient()) {
                    tankCoolant.fillInternal(solid.fluid(), false);
                    if (!player.isCreative()) held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (FluidUtilBC.interactWithHandler(player, hand, fluidHandler)) {
            return InteractionResult.SUCCESS;
        }
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public double getPistonSpeed() {
        return switch (getPowerStage()) {
            case BLUE -> 0.04;
            case GREEN -> 0.05;
            case YELLOW -> 0.06;
            case RED -> 0.07;
            default -> 0;
        };
    }

    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return !tankFuel.isEmpty() && penaltyCooling == 0 && isRedstonePowered;
    }

    @Override
    protected void burn() {
        BCFluidStack fuel = tankFuel.getFluid();
        if (currentFuel == null || !fuel.isSameFluid(currentFuel.fluid())) {
            currentFuel = BuildcraftFuelRegistry.getFuel(fuel);
        }
        if (fuel.isEmpty() || currentFuel == null) {
            return;
        }

        if (penaltyCooling <= 0) {
            if (isRedstonePowered) {
                lastPowered = true;
                if (burnTime > 0 || !tankFuel.isEmpty()) {
                    if (burnTime > 0) {
                        burnTime--;
                    }
                    if (burnTime <= 0) {
                        if (!tankFuel.isEmpty()) {
                            tankFuel.drainInternal(1, false);
                            burnTime += currentFuel.totalBurningTime() / 1000.0;
                            // If we also produce residue then put it out too
                            if (currentFuel.isDirty()) {
                                BCFluidStack residue = currentFuel.residue();
                                residueAmount += residue.getAmount() / 1000.0;
                                if (residueAmount >= 1) {
                                    int whole = Mth.floor(residueAmount);
                                    residueAmount -= tankResidue.fillInternal(residue.withAmount(whole), false);
                                }
                            }
                        } else {
                            currentFuel = null;
                            currentOutput = 0;
                            return;
                        }
                    }
                    currentOutput = currentFuel.powerPerCycle();
                    addPower(currentFuel.powerPerCycle());
                    heat += currentFuel.powerPerCycle() * HEAT_PER_MJ / MjAPI.MJ;
                }
            } else if (lastPowered) {
                lastPowered = false;
                // 10 ticks of penalty on top of the cooling
                penaltyCooling = 10;
            }
        }
    }

    @Override
    public void updateHeatLevel() {
        double target;
        if (heat > MIN_HEAT && (penaltyCooling > 0 || !isRedstonePowered)) {
            heat -= COOLDOWN_RATE;
            target = MIN_HEAT;
        } else if (heat > IDEAL_HEAT) {
            target = IDEAL_HEAT;
        } else {
            target = heat;
        }

        if (target != heat) {
            double extraHeat = heat - target;
            if (extraHeat > 0 && tankCoolant.getFluidAmount() > 0) {
                float coolPerMb = BuildcraftFuelRegistry.getDegreesPerMb(tankCoolant.getFluid(), (float) heat);
                if (coolPerMb > 0) {
                    int coolantAmount = Math.min(MAX_COOLANT_PER_TICK, tankCoolant.getFluidAmount());
                    heat -= coolantAmount * coolPerMb;
                    tankCoolant.drainInternal(coolantAmount, false);
                }
            }
            getPowerStage();
        }

        if (heat <= MIN_HEAT && penaltyCooling > 0) {
            penaltyCooling--;
        }
        if (heat <= MIN_HEAT) {
            heat = MIN_HEAT;
        }
    }

    @Override
    public boolean isActive() {
        return penaltyCooling <= 0;
    }

    @Override
    public long getMaxPower() {
        return 10_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerReceived() {
        return 2_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 500 * MjAPI.MJ;
    }

    @Override
    public float explosionRange() {
        return 4;
    }

    @Override
    protected int getMaxChainLength() {
        return 4;
    }

    @Override
    public long getCurrentOutput() {
        return currentFuel == null ? 0 : currentFuel.powerPerCycle();
    }

    // Fluids

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return fluidHandler;
    }

    /** Fuel and coolant go in, residue comes out. */
    private final class ExternalFluidHandler implements IFluidHandlerBC, ISnapshotable {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public BCFluidStack getFluidInTank(int tank) {
            return tankManager.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return MAX_FLUID;
        }

        @Override
        public boolean isFluidValid(int tank, BCFluidStack stack) {
            return tank < 2 && tankManager.isFluidValid(tank, stack);
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            int filled = tankFuel.fill(resource, simulate);
            if (filled == 0) {
                filled = tankCoolant.fill(resource, simulate);
            }
            return filled;
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            return tankResidue.drain(resource, simulate);
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            return tankResidue.drain(filter, maxDrain, simulate);
        }

        @Override
        public Object createSnapshot() {
            return tankManager.createSnapshot();
        }

        @Override
        public void restoreSnapshot(Object snapshot) {
            tankManager.restoreSnapshot(snapshot);
        }

        @Override
        public void onSnapshotCommit() {
            setChanged();
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerEngineIron(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) buildcraft.core.item.ItemFragileFluidShard.dropFluids(level, pos, tankManager);
    }
}
