/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.api.power.ILaserTarget;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.lib.tile.TileBC;

/** Sends power from its battery to laser tables in front of it. */
public class TileLaser extends TileBC implements IMjConnectorProvider {
    private static final int TARGETING_RANGE = 6;
    private static final int TARGET_SEARCH_INTERVAL = 40;
    private static final int TARGET_CHANGE_INTERVAL = 20;
    public static final long MAX_POWER_PER_TICK = 4 * MjAPI.MJ;

    private final MjBattery battery = new MjBattery(1024 * MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);
    private final List<BlockPos> targetPositions = new ArrayList<>();
    @Nullable
    private BlockPos targetPos;
    private int searchTimer = 0;
    private int changeTimer = 0;
    /** The average power sent each tick, for choosing the colour of the laser. */
    private long averagePower = 0;
    private long lastSentPower = -1;

    // Client side: where on the target the beam hits. It wanders around a little.
    @Nullable
    public Vec3 laserPos;
    private int laserMoveTimer = 0;

    public TileLaser(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.LASER_TILE.get(), pos, state);
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    @Nullable
    public BlockPos getTargetPos() {
        return targetPos;
    }

    public long getAveragePower() {
        return averagePower;
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockLaser.FACING) ? state.getValue(BlockLaser.FACING) : Direction.UP;
    }

    /** Finds every laser target in a cone in front of the laser. */
    private void findPossibleTargets() {
        targetPositions.clear();
        if (level == null) return;
        Direction facing = getFacing();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-TARGETING_RANGE, -TARGETING_RANGE, -TARGETING_RANGE),
            worldPosition.offset(TARGETING_RANGE, TARGETING_RANGE, TARGETING_RANGE))) {
            BlockPos diff = p.subtract(worldPosition);
            int forward = diff.getX() * facing.getStepX() + diff.getY() * facing.getStepY() + diff.getZ() * facing.getStepZ();
            if (forward <= 0) continue;
            int sideways = Math.abs(diff.getX()) + Math.abs(diff.getY()) + Math.abs(diff.getZ()) - forward;
            if (sideways > forward * 2) continue;
            if (level.getBlockEntity(p) instanceof ILaserTarget) {
                targetPositions.add(p.immutable());
            }
        }
    }

    private boolean isPowerNeededAt(@Nullable BlockPos pos) {
        return pos != null && level != null && level.getBlockEntity(pos) instanceof ILaserTarget target
            && target.getRequiredLaserPower() > 0;
    }

    private void chooseTarget() {
        List<BlockPos> needing = new ArrayList<>();
        for (BlockPos p : targetPositions) {
            if (isPowerNeededAt(p)) needing.add(p);
        }
        targetPos = needing.isEmpty() || level == null ? null : needing.get(level.getRandom().nextInt(needing.size()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide()) {
            if (targetPos == null) {
                laserPos = null;
            } else if (laserPos == null || ++laserMoveTimer >= 5) {
                laserMoveTimer = 0;
                laserPos = Vec3.atLowerCornerOf(targetPos).add((5 + level.getRandom().nextInt(6) + 0.5) / 16, 9 / 16.0,
                    (5 + level.getRandom().nextInt(6) + 0.5) / 16);
            }
            return;
        }
        battery.tick();
        BlockPos previous = targetPos;
        if (--searchTimer <= 0) {
            searchTimer = TARGET_SEARCH_INTERVAL;
            findPossibleTargets();
        }
        if (!isPowerNeededAt(targetPos)) {
            targetPos = null;
        }
        if (++changeTimer >= TARGET_CHANGE_INTERVAL || targetPos == null) {
            changeTimer = 0;
            chooseTarget();
        }
        long sent = 0;
        if (targetPos != null && level.getBlockEntity(targetPos) instanceof ILaserTarget target) {
            // Lasers fire faster when their battery is fuller
            long max = MAX_POWER_PER_TICK;
            max *= battery.getStored() + max;
            max /= battery.getCapacity() / 2;
            max = Math.min(Math.min(max, MAX_POWER_PER_TICK), target.getRequiredLaserPower());
            long power = battery.extractPower(0, max);
            long excess = target.receiveLaserPower(power);
            if (excess > 0) {
                battery.addPowerChecking(excess, false);
            }
            sent = power - excess;
        }
        averagePower += (sent - averagePower) / 10;
        long rounded = averagePower / (MjAPI.MJ / 4);
        if (!Objects.equals(previous, targetPos) || rounded != lastSentPower) {
            lastSentPower = rounded;
            sendNetworkUpdate();
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        battery.save(output, "battery");
        if (targetPos != null) {
            output.store("target", BlockPos.CODEC, targetPos);
        }
        output.putLong("average", averagePower);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.load(input, "battery");
        targetPos = input.read("target", BlockPos.CODEC).orElse(null);
        averagePower = input.getLongOr("average", 0);
    }
}
