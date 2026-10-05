/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.lib.misc.RotationUtil;
import buildcraft.lib.tile.TileBC;

public abstract class TileEngineBase extends TileBC implements IMjConnectorProvider {
    /** Heat per {@link MjAPI#MJ}. */
    public static final double HEAT_PER_MJ = 0.0023;

    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;

    public final IMjConnector mjConnector = createConnector();

    protected double heat = MIN_HEAT;
    protected long power = 0;
    private long lastPower = 0;
    /** Increments from 0 to 1. Above 0.5 all of the held power is emitted. */
    private float progress, lastProgress;
    private int progressPart = 0;

    protected EnumPowerStage powerStage = EnumPowerStage.BLUE;

    public long currentOutput;
    public boolean isRedstonePowered = false;
    protected boolean isPumping = false;
    private boolean checkRedstone = true;

    protected TileEngineBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isRedstonePowered = input.getBooleanOr("isRedstonePowered", false);
        heat = input.getDoubleOr("heat", MIN_HEAT);
        power = input.getLongOr("power", 0);
        progress = input.getFloatOr("progress", 0);
        progressPart = input.getIntOr("progressPart", 0);
        isPumping = input.getBooleanOr("isPumping", false);
        currentOutput = input.getLongOr("currentOutput", 0);
        powerStage = EnumPowerStage.VALUES[input.getIntOr("stage", 0) % EnumPowerStage.VALUES.length];
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isRedstonePowered", isRedstonePowered);
        output.putDouble("heat", heat);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        output.putInt("progressPart", progressPart);
        output.putBoolean("isPumping", isPumping);
        output.putLong("currentOutput", currentOutput);
        output.putInt("stage", powerStage.ordinal());
    }

    public Direction getCurrentFacing() {
        return getBlockState().getValue(BlockEngine.FACING);
    }

    private void setFacing(Direction facing) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getValue(BlockEngine.FACING) != facing) {
            level.setBlock(worldPosition, state.setValue(BlockEngine.FACING, facing), Block.UPDATE_ALL);
        }
    }

    /** Rotates to the next direction that has a receiver. */
    public boolean attemptRotation() {
        Direction current = getCurrentFacing();
        Direction next = current;
        for (int i = 0; i < 6; i++) {
            next = RotationUtil.next(next);
            if (isFacingReceiver(next)) {
                if (next != current) {
                    setFacing(next);
                    sendNetworkUpdate();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private boolean isFacingReceiver(Direction dir) {
        return getReceiverToPower(dir) != null;
    }

    /** @return The number of additional engines that this engine can send power through. */
    protected int getMaxChainLength() {
        return 2;
    }

    public void rotateIfInvalid() {
        if (!isFacingReceiver(getCurrentFacing())) {
            attemptRotation();
        }
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        rotateIfInvalid();
        checkRedstone = true;
    }

    /** @return The heat of the current biome, in celsius. */
    protected float getBiomeHeat() {
        if (level == null) return 15;
        float temp = level.getBiome(worldPosition).value().getBaseTemperature();
        return Math.max(0, Math.min(30, temp * 15f));
    }

    public double getPowerLevel() {
        return power / (double) getMaxPower();
    }

    protected EnumPowerStage computePowerStage() {
        double heatLevel = getHeatLevel();
        if (heatLevel < 0.25f) return EnumPowerStage.BLUE;
        else if (heatLevel < 0.5f) return EnumPowerStage.GREEN;
        else if (heatLevel < 0.75f) return EnumPowerStage.YELLOW;
        else if (heatLevel < 0.85f) return EnumPowerStage.RED;
        else return EnumPowerStage.OVERHEAT;
    }

    public final EnumPowerStage getPowerStage() {
        if (level != null && !level.isClientSide()) {
            EnumPowerStage newStage = computePowerStage();
            if (powerStage != newStage) {
                powerStage = newStage;
                BlockState state = getBlockState();
                if (state.hasProperty(BlockEngine.STAGE) && state.getValue(BlockEngine.STAGE) != newStage) {
                    level.setBlock(worldPosition, state.setValue(BlockEngine.STAGE, newStage), Block.UPDATE_CLIENTS);
                }
                sendNetworkUpdate();
            }
        }
        return powerStage;
    }

    public void updateHeatLevel() {
        heat = ((MAX_HEAT - MIN_HEAT) * getPowerLevel()) + MIN_HEAT;
    }

    public double getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public double getHeat() {
        return heat;
    }

    public double getPistonSpeed() {
        return switch (getPowerStage()) {
            case BLUE -> 0.02;
            case GREEN -> 0.04;
            case YELLOW -> 0.08;
            case RED -> 0.12;
            default -> 0;
        };
    }

    protected abstract IMjConnector createConnector();

    @Override
    public void onNeighbourChanged() {
        checkRedstone = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide()) {
            lastProgress = progress;
            if (isPumping) {
                progress += (float) getPistonSpeed();
                if (progress >= 1) {
                    progress = 0;
                }
            } else if (progress > 0) {
                progress -= 0.01f;
                if (progress < 0) progress = 0;
            }
            return;
        }

        if (checkRedstone) {
            checkRedstone = false;
            boolean powered = level.hasNeighborSignal(worldPosition);
            if (powered != isRedstonePowered) {
                isRedstonePowered = powered;
                sendNetworkUpdate();
            }
        }

        boolean overheat = getPowerStage() == EnumPowerStage.OVERHEAT;
        lastPower = 0;

        if (!isRedstonePowered) {
            if (power > MjAPI.MJ) {
                power -= MjAPI.MJ;
            } else if (power > 0) {
                power = 0;
            }
        }

        updateHeatLevel();
        getPowerStage();
        engineUpdate();

        IMjReceiver receiver = getReceiverToPower(getCurrentFacing());
        boolean pulsedPower = receiver instanceof IMjRedstoneReceiver;

        if (progressPart != 0) {
            progress += (float) getPistonSpeed();

            if (progress > 0.5 && progressPart == 1) {
                progressPart = 2;
                if (pulsedPower) {
                    sendPower(receiver);
                }
            } else if (progress >= 1) {
                progress = 0;
                progressPart = 0;
            }
        } else if (isRedstonePowered && isActive()) {
            if (getPowerToExtract(false) > 0) {
                progressPart = 1;
                setPumping(true);
            } else {
                setPumping(false);
            }
        } else {
            setPumping(false);
        }

        if (!pulsedPower) {
            if (isRedstonePowered && isActive()) {
                sendPower(receiver);
            } else {
                currentOutput = 0;
            }
        }

        if (!overheat) {
            burn();
        }

        setChanged();
    }

    private long getPowerToExtract(boolean doExtract) {
        IMjReceiver receiver = getReceiverToPower(getCurrentFacing());
        if (receiver == null) {
            return 0;
        }
        return extractPower(0, receiver.getPowerRequested(), doExtract);
    }

    private void sendPower(@Nullable IMjReceiver receiver) {
        if (receiver != null) {
            long extracted = getPowerToExtract(false);
            if (extracted > 0) {
                long excess = receiver.receivePower(extracted, false);
                extractPower(extracted - excess, extracted - excess, true);
            }
        }
    }

    protected void burn() {}

    protected void engineUpdate() {
        if (!isRedstonePowered) {
            if (power >= 1) {
                power -= 1;
            } else {
                power = 0;
            }
        }
    }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean isActive) {
        if (this.isPumping == isActive) {
            return;
        }
        this.isPumping = isActive;
        sendNetworkUpdate();
    }

    public abstract boolean isBurning();

    public void addPower(long microJoules) {
        power += microJoules;
        lastPower += microJoules;
        if (power > getMaxPower()) {
            power = getMaxPower();
        }
    }

    public long extractPower(long min, long max, boolean doExtract) {
        if (power < min) {
            return 0;
        }
        long actualMax = Math.min(max, maxPowerExtracted());
        if (actualMax < min) {
            return 0;
        }
        long extracted;
        if (power >= actualMax) {
            extracted = actualMax;
            if (doExtract) {
                power -= actualMax;
            }
        } else {
            extracted = power;
            if (doExtract) {
                power = 0;
            }
        }
        return extracted;
    }

    @Nullable
    private IMjReceiver getReceiverToPower(BlockEntity tile, Direction side) {
        IMjReceiver rec = MjAPI.getReceiver(tile.getLevel(), tile.getBlockPos(), side.getOpposite());
        if (rec != null && rec.canConnect(mjConnector) && mjConnector.canConnect(rec)) {
            return rec;
        }
        return null;
    }

    /** Finds the receiver this engine powers, following a chain of up to {@link #getMaxChainLength()} engines of
     * the same type that face the same way. */
    @Nullable
    public IMjReceiver getReceiverToPower(Direction side) {
        if (level == null) return null;
        BlockPos pos = worldPosition;
        BlockEntity next = null;

        for (int len = 0; len <= getMaxChainLength(); len++) {
            pos = pos.relative(side);
            next = level.getBlockEntity(pos);
            if (next == null) {
                return null;
            }
            if (next instanceof TileEngineBase engine) {
                if (next.getClass() != getClass() || engine.getCurrentFacing() != side) {
                    return null;
                }
            } else {
                break;
            }
        }

        if (next == null || next instanceof TileEngineBase) {
            return null;
        }
        return getReceiverToPower(next, side);
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return side == getCurrentFacing() ? mjConnector : null;
    }

    public abstract long getMaxPower();

    public long minPowerReceived() {
        return 2 * MjAPI.MJ;
    }

    public abstract long maxPowerReceived();

    public abstract long maxPowerExtracted();

    public abstract float explosionRange();

    public long getEnergyStored() {
        return power;
    }

    public abstract long getCurrentOutput();

    public boolean isEngineOn() {
        return isPumping;
    }

    public float getProgressClient(float partialTicks) {
        float last = lastProgress;
        float now = progress;
        if (last > 0.5 && now < 0.5) {
            // we just returned
            now += 1;
        }
        float interp = last * (1 - partialTicks) + now * partialTicks;
        return interp % 1;
    }

    public void getDebugInfo(List<String> left) {
        left.add("facing = " + getCurrentFacing());
        left.add("heat = " + String.format("%.1f", heat) + " -- " + String.format("%.2f %%", getHeatLevel()));
        left.add("power = " + MjAPI.formatMj(power) + " MJ");
        left.add("stage = " + powerStage);
        left.add("progress = " + progress);
        left.add("last = " + MjAPI.formatMj(lastPower) + " MJ/t");
    }
}
