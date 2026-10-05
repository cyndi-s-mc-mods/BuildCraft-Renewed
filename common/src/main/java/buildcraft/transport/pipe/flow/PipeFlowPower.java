/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjPassiveProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowPower;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeEventPower;
import buildcraft.api.transport.pipe.PipeFlow;

/** Kinesis pipes: carry MJ from engines to machines. */
public class PipeFlowPower extends PipeFlow implements IFlowPower {
    private static final long DEFAULT_MAX_POWER = MjAPI.MJ * 10;
    private static final int NETWORK_UPDATE_RATE = 4;

    private long maxPower = -1;
    private boolean disabled = false;
    private long currentWorldTime;
    private boolean isReceiver = false;
    private final Map<Direction, Section> sections = new EnumMap<>(Direction.class);
    private long lastSync = 0;
    private boolean needsSync = false;

    public PipeFlowPower(IPipe pipe) {
        super(pipe);
        for (Direction face : Direction.values()) {
            sections.put(face, new Section(face));
        }
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        output.putBoolean("isReceiver", isReceiver);
        int[] display = new int[12];
        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            display[face.get3DDataValue() * 2] = s.displayPower;
            display[face.get3DDataValue() * 2 + 1] = s.displayFlow;
        }
        output.putIntArray("display", display);
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        isReceiver = input.getBooleanOr("isReceiver", false);
        int[] display = input.getIntArray("display").orElse(new int[12]);
        if (display.length == 12) {
            for (Direction face : Direction.values()) {
                Section s = sections.get(face);
                s.displayPower = display[face.get3DDataValue() * 2];
                s.displayFlow = display[face.get3DDataValue() * 2 + 1];
            }
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof PipeFlowPower;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        Level level = oTile.getLevel();
        if (level == null) return false;
        IMjConnector other = MjAPI.getConnector(level, oTile.getBlockPos(), face.getOpposite());
        if (other == null) return false;
        if (isReceiver && other instanceof IMjPassiveProvider) {
            return true;
        }
        return other.canConnect(sections.get(face));
    }

    @Override
    public void reconfigure() {
        PipeEventPower.Configure configure = new PipeEventPower.Configure(pipe.getHolder(), this);
        PipeApi.PowerTransferInfo pti = PipeApi.getPowerTransferInfo(pipe.getDefinition());
        configure.setReceiver(pti.isReceiver());
        configure.setMaxPower(pti.transferPerTick());
        configure.setPowerLoss(pti.lossPerTick());
        configure.setPowerResistance(pti.resistancePerTick());
        pipe.getHolder().fireEvent(configure);
        isReceiver = configure.isReceiver();
        maxPower = configure.getMaxPower();
        disabled = configure.isTransferDisabled();
        if (maxPower <= 0) {
            maxPower = DEFAULT_MAX_POWER;
        }
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return sections.get(side);
    }

    public long getMaxPower() {
        if (maxPower == -1) {
            reconfigure();
        }
        return maxPower;
    }

    /** @return How much power is shown flowing through a side, from 0 to {@link MjAPI#MJ}. */
    /** @return True if power is flowing through any side of the pipe. */
    public boolean hasPower() {
        for (Section s : sections.values()) {
            if (s.displayPower > 0) return true;
        }
        return false;
    }

    public int getDisplayPower(Direction side) {
        return sections.get(side).displayPower;
    }

    /** @return -1 if power flows in through the side, 1 if out, 0 if none. */
    public int getDisplayFlow(Direction side) {
        return sections.get(side).displayFlow;
    }

    @Override
    public void onTick() {
        if (maxPower == -1) {
            reconfigure();
        }
        Level level = pipe.getHolder().getPipeWorld();
        if (level.isClientSide()) {
            return;
        }

        int[] lastFlows = new int[6];
        int[] lastDisplayPower = new int[6];
        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            lastFlows[face.get3DDataValue()] = s.displayFlow;
            lastDisplayPower[face.get3DDataValue()] = s.displayPower;
            s.displayFlow = 0;
        }

        step();

        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            if (s.internalPower <= 0) continue;
            long totalPowerQuery = 0;
            for (Direction face2 : Direction.values()) {
                if (face != face2) {
                    totalPowerQuery += sections.get(face2).powerQuery;
                }
            }
            boolean returnPower = false;
            if (totalPowerQuery <= 0 && s.powerQuery > 0) {
                totalPowerQuery = s.powerQuery;
                returnPower = true;
            }
            if (totalPowerQuery <= 0) continue;
            long unusedPowerQuery = totalPowerQuery;
            for (Direction face2 : Direction.values()) {
                if (face == face2 && !returnPower) {
                    continue;
                }
                Section s2 = sections.get(face2);
                if (s2.powerQuery <= 0) continue;
                long watts = Math.min(BigInteger.valueOf(s.internalPower).multiply(BigInteger.valueOf(s2.powerQuery))
                    .divide(BigInteger.valueOf(unusedPowerQuery)).longValue(), s.internalPower);
                unusedPowerQuery -= s2.powerQuery;
                IPipe neighbour = pipe.getConnectedPipe(face2);
                long leftover = watts;
                if (neighbour != null && neighbour.getFlow() instanceof PipeFlowPower oFlow
                    && neighbour.isConnected(face2.getOpposite())) {
                    leftover = oFlow.sections.get(face2.getOpposite()).receivePowerInternal(watts);
                } else {
                    IMjReceiver receiver = getReceiver(face2);
                    if (receiver != null && receiver.canReceive()) {
                        leftover = receiver.receivePower(watts, false);
                    }
                }
                long used = watts - leftover;
                s.internalPower -= used;
                s.powerAverage += used;
                s2.powerAverage += used;
                if (used > 0) {
                    s.displayFlow = -1;
                    s2.displayFlow = 1;
                }
            }
        }

        for (Section s : sections.values()) {
            // Smooth the displayed power over a few ticks
            s.displayAverage = s.displayAverage * 0.7 + s.powerAverage * 0.3;
            s.powerAverage = 0;
            double value = Math.sqrt(s.displayAverage / (double) maxPower);
            s.displayPower = (int) (Mth.clamp(value, 0, 1) * MjAPI.MJ);
        }

        // Ask the machines we are connected to how much power they want
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) != ConnectedType.TILE) {
                continue;
            }
            IMjReceiver recv = getReceiver(face);
            if (recv != null && recv.canReceive()) {
                long requested = recv.getPowerRequested();
                if (requested > 0) {
                    requestPower(face, requested);
                }
            }
        }

        // And pass those requests on to neighbouring pipes
        long[] transferQuery = new long[6];
        for (Direction face : Direction.values()) {
            if (!pipe.isConnected(face)) {
                continue;
            }
            long query = 0;
            for (Direction face2 : Direction.values()) {
                if (face != face2) {
                    query += sections.get(face2).powerQuery;
                }
            }
            transferQuery[face.get3DDataValue()] = query;
        }
        for (Direction face : Direction.values()) {
            if (disabled || transferQuery[face.get3DDataValue()] <= 0 || !pipe.isConnected(face)) {
                continue;
            }
            IPipe oPipe = pipe.getHolder().getNeighbourPipe(face);
            if (oPipe != null && oPipe.getFlow() instanceof PipeFlowPower oFlow) {
                oFlow.requestPower(face.getOpposite(), transferQuery[face.get3DDataValue()]);
            }
        }

        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            int i = face.get3DDataValue();
            if (lastFlows[i] != s.displayFlow || Math.abs(lastDisplayPower[i] - s.displayPower) > MjAPI.MJ / 50) {
                needsSync = true;
                break;
            }
        }
        if (needsSync && level.getGameTime() - lastSync >= NETWORK_UPDATE_RATE) {
            needsSync = false;
            lastSync = level.getGameTime();
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }

    private @Nullable IMjReceiver getReceiver(Direction side) {
        return MjAPI.getReceiver(pipe.getHolder().getPipeWorld(), pipe.getHolder().getPipePos().relative(side), side.getOpposite());
    }

    private void step() {
        long now = pipe.getHolder().getPipeWorld().getGameTime();
        if (currentWorldTime != now) {
            currentWorldTime = now;
            for (Section section : sections.values()) {
                section.step();
            }
        }
    }

    private void requestPower(Direction from, long amount) {
        step();
        Section s = sections.get(from);
        s.nextPowerQuery = Math.min(s.nextPowerQuery + amount, getMaxPower());
    }

    long getPowerRequested(@Nullable Direction side) {
        long req = 0;
        for (Direction face : Direction.values()) {
            if (side == null || face != side) {
                req += sections.get(face).powerQuery;
            }
        }
        return req;
    }

    public class Section implements IMjReceiver {
        public final Direction side;
        int displayPower;
        /** -1 for in, 1 for out, 0 for none. */
        int displayFlow;
        long nextPowerQuery;
        long internalNextPower;
        long powerQuery;
        long internalPower;
        long powerAverage;
        double displayAverage;

        Section(Direction side) {
            this.side = side;
        }

        void step() {
            powerQuery = nextPowerQuery;
            nextPowerQuery = 0;
            internalPower += internalNextPower;
            internalNextPower = 0;
        }

        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return PipeFlowPower.this.getPowerRequested(side);
        }

        long receivePowerInternal(long sent) {
            if (sent > 0) {
                PipeFlowPower.this.step();
                internalNextPower += sent;
                return 0;
            }
            return sent;
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (isReceiver) {
                if (!simulate) {
                    return receivePowerInternal(microJoules);
                }
                return 0;
            }
            return microJoules;
        }

        @Override
        public boolean canReceive() {
            return isReceiver;
        }
    }
}
