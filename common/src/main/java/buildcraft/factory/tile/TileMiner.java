/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.tile.TileBC;

/** Base for machines that lower a tube into the ground: the mining well and the pump. */
public abstract class TileMiner extends TileBC implements IMjConnectorProvider, IHasWork, IControllable {
    protected int progress = 0;
    @Nullable
    protected BlockPos currentPos = null;
    /** How far down the tube reaches. */
    private int tubeLength = 0;
    protected boolean isComplete = false;
    protected final MjBattery battery = new MjBattery(getBatteryCapacity());
    private final IMjReceiver receiver = createMjReceiver();

    protected TileMiner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void mine();

    protected abstract IMjReceiver createMjReceiver();

    protected long getBatteryCapacity() {
        return 500 * MjAPI.MJ;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            battery.tick();
            if (controlMode != IControllable.Mode.OFF) {
                mine();
            }
            boolean complete = currentPos == null;
            if (complete != isComplete) {
                isComplete = complete;
                sendNetworkUpdate();
            }
        }
        super.tick();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        removeTube();
    }

    private void removeTube() {
        if (level == null) return;
        for (int y = worldPosition.getY() - 1; y > worldPosition.getY() - BlockUtil.MINING_MAX_DEPTH; y--) {
            BlockPos p = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockState(p).is(BCFactoryBlocks.TUBE.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                break;
            }
        }
    }

    /** Extends or retracts the tube so that it reaches just above {@link #getTargetPos()}. */
    protected void updateLength() {
        if (level == null) return;
        BlockPos target = getTargetPos();
        int newY = target != null ? target.getY() : worldPosition.getY();
        int newLength = worldPosition.getY() - newY;
        if (newLength != tubeLength) {
            removeTube();
            BlockState tube = BCFactoryBlocks.TUBE.get().defaultBlockState();
            for (int y = worldPosition.getY() - 1; y > newY; y--) {
                BlockPos p = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
                if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                    level.setBlock(p, tube, Block.UPDATE_ALL);
                } else {
                    break;
                }
            }
            tubeLength = newLength;
            setChanged();
        }
    }

    @Nullable
    protected BlockPos getTargetPos() {
        return currentPos;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public long getBatteryStored() {
        return battery.getStored();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (currentPos != null) {
            output.putLong("currentPos", currentPos.asLong());
        }
        output.putInt("tubeLength", tubeLength);
        output.putInt("progress", progress);
        output.putBoolean("complete", isComplete);
        battery.save(output, "battery");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        currentPos = input.getLong("currentPos").map(BlockPos::of).orElse(null);
        tubeLength = input.getIntOr("tubeLength", 0);
        progress = input.getIntOr("progress", 0);
        isComplete = input.getBooleanOr("complete", false);
        battery.load(input, "battery");
    }

    // Gates

    private IControllable.Mode controlMode = IControllable.Mode.UNKNOWN;

    @Override
    public IControllable.Mode getControlMode() {
        return controlMode;
    }

    @Override
    public void setControlMode(IControllable.Mode mode) {
        controlMode = mode;
    }

    @Override
    public boolean acceptsControlMode(IControllable.Mode mode) {
        return mode == IControllable.Mode.ON || mode == IControllable.Mode.OFF;
    }

    @Override
    public boolean hasWork() {
        return !isComplete;
    }
}
