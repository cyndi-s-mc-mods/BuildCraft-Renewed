/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.transfer.ISnapshotable;

public class PipeFlowFluids extends PipeFlow implements IFlowFluid {
    private static final int DIRECTION_COOLDOWN = 60;
    private static final int COOLDOWN_INPUT = -DIRECTION_COOLDOWN;
    private static final int COOLDOWN_OUTPUT = DIRECTION_COOLDOWN;
    /** Minimum ticks between sending fluid amounts to the client. */
    private static final int NETWORK_UPDATE_RATE = 4;

    private final PipeApi.FluidTransferInfo fluidTransferInfo;
    public final int capacity;
    private final Map<EnumPipePart, Section> sections = new EnumMap<>(EnumPipePart.class);
    private BCFluidStack currentFluid = BCFluidStack.EMPTY;
    private int currentDelay;
    private long lastSync = 0;
    private boolean needsSync = false;

    public PipeFlowFluids(IPipe pipe) {
        super(pipe);
        fluidTransferInfo = PipeApi.getFluidTransferInfo(pipe.getDefinition());
        capacity = Math.max(BCFluidStack.BUCKET, fluidTransferInfo.transferPerTick() * 10);
        for (EnumPipePart part : EnumPipePart.VALUES) {
            sections.put(part, new Section(part));
        }
        setFluid(BCFluidStack.EMPTY);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        if (!currentFluid.isEmpty()) {
            currentFluid.save(output.child("fluid"));
            for (EnumPipePart part : EnumPipePart.VALUES) {
                sections.get(part).save(output.child("tank_" + part.getSerializedName()));
            }
        }
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        boolean client = isClient();
        BCFluidStack fluid = input.child("fluid").map(BCFluidStack::load).orElse(BCFluidStack.EMPTY);
        if (!fluid.isSameFluid(currentFluid) || fluid.isEmpty()) {
            setFluid(fluid);
        } else {
            currentFluid = fluid;
        }
        for (EnumPipePart part : EnumPipePart.VALUES) {
            Section section = sections.get(part);
            ValueInput child = input.child("tank_" + part.getSerializedName()).orElse(null);
            if (child == null) {
                section.amount = 0;
                section.target = 0;
                continue;
            }
            section.load(child);
            if (client) {
                section.target = section.amount;
            } else {
                section.clientAmountThis = section.clientAmountLast = section.amount;
            }
        }
    }

    private boolean isClient() {
        Level level = pipe.getHolder().getPipeWorld();
        return level != null && level.isClientSide();
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof IFlowFluid;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        Level level = oTile.getLevel();
        return level != null && Platform.INSTANCE.getFluidHandler(level, oTile.getBlockPos(), face.getOpposite()) != null;
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return side == null ? null : sections.get(EnumPipePart.fromFacing(side));
    }

    public boolean doesContainFluid() {
        for (Section section : sections.values()) {
            if (section.amount > 0) return true;
        }
        return false;
    }

    private @Nullable IFluidHandlerBC getNeighbourHandler(Direction side) {
        return Platform.INSTANCE.getFluidHandler(pipe.getHolder().getPipeWorld(), pipe.getHolder().getPipePos().relative(side),
            side.getOpposite());
    }

    @Override
    public BCFluidStack tryExtractFluid(int millibuckets, Direction from, Predicate<BCFluidStack> filter, boolean simulate) {
        if (millibuckets <= 0) {
            return BCFluidStack.EMPTY;
        }
        IFluidHandlerBC fluidHandler = getNeighbourHandler(from);
        if (fluidHandler == null) {
            return BCFluidStack.EMPTY;
        }
        Section section = sections.get(EnumPipePart.fromFacing(from));
        Section middle = sections.get(EnumPipePart.CENTER);
        millibuckets = Math.min(millibuckets, capacity * 2 - section.amount - middle.amount);
        if (millibuckets <= 0) {
            return BCFluidStack.EMPTY;
        }
        BCFluidStack toAdd;
        if (currentFluid.isEmpty()) {
            toAdd = fluidHandler.drain(filter, millibuckets, simulate);
        } else if (filter.test(currentFluid)) {
            toAdd = fluidHandler.drain(currentFluid.withAmount(millibuckets), simulate);
        } else {
            return BCFluidStack.EMPTY;
        }
        if (toAdd.isEmpty()) {
            return BCFluidStack.EMPTY;
        }
        millibuckets = toAdd.getAmount();
        if (currentFluid.isEmpty() && !simulate) {
            setFluid(toAdd);
        }
        int reallyFilled = section.fillInternal(millibuckets, !simulate);
        int leftOver = millibuckets - reallyFilled;
        middle.fillInternal(leftOver, !simulate);
        if (!simulate) {
            section.ticksInDirection = COOLDOWN_INPUT;
        }
        return toAdd;
    }

    /** Puts fluid straight into the centre of the pipe. */
    public int insertFluidsForce(BCFluidStack fluid, @Nullable Direction from, boolean simulate) {
        Section s = sections.get(EnumPipePart.CENTER);
        if (fluid.isEmpty()) {
            return 0;
        }
        if (!currentFluid.isEmpty() && !currentFluid.isSameFluid(fluid)) {
            return 0;
        }
        if (currentFluid.isEmpty() && !simulate) {
            setFluid(fluid);
        }
        int filled = s.fill(fluid.getAmount(), !simulate);
        if (filled > 0 && !simulate && from != null) {
            sections.get(EnumPipePart.fromFacing(from)).ticksInDirection = COOLDOWN_INPUT;
        }
        return filled;
    }

    private void setFluid(BCFluidStack fluid) {
        currentFluid = fluid.isEmpty() ? BCFluidStack.EMPTY : fluid.withAmount(1);
        currentDelay = (int) fluidTransferInfo.transferDelayMultiplier();
        for (Section section : sections.values()) {
            section.incoming = new int[currentDelay];
            section.incomingTotalCache = 0;
            section.currentTime = 0;
            section.ticksInDirection = 0;
        }
    }

    public BCFluidStack getFluidForRender() {
        return currentFluid;
    }

    /** @return The amount in each section (indexed by {@link EnumPipePart#getIndex()}), interpolated for rendering. */
    public double[] getAmountsForRender(float partialTicks) {
        double[] arr = new double[7];
        for (EnumPipePart part : EnumPipePart.VALUES) {
            Section s = sections.get(part);
            arr[part.getIndex()] = s.clientAmountLast * (1 - partialTicks) + s.clientAmountThis * partialTicks;
        }
        return arr;
    }

    @Override
    public void onTick() {
        Level level = pipe.getHolder().getPipeWorld();
        if (level.isClientSide()) {
            for (Section section : sections.values()) {
                section.tickClient();
            }
            return;
        }

        if (!currentFluid.isEmpty()) {
            int totalFluid = 0;
            boolean canOutput = false;
            for (Section section : sections.values()) {
                section.currentTime = (section.currentTime + 1) % currentDelay;
                section.advanceForMovement();
                totalFluid += section.amount;
                if (section.getCurrentDirection().canOutput()) {
                    canOutput = true;
                }
            }
            if (totalFluid == 0) {
                setFluid(BCFluidStack.EMPTY);
            } else {
                if (canOutput) {
                    moveFromPipe();
                }
                moveFromCenter();
                moveToCenter();
            }
            for (Section section : sections.values()) {
                if (section.ticksInDirection > 0) {
                    section.ticksInDirection--;
                } else if (section.ticksInDirection < 0) {
                    section.ticksInDirection++;
                }
            }
        }

        for (Section section : sections.values()) {
            if (section.amount != section.lastSentAmount || section.lastSentDirection != section.getCurrentDirection()) {
                needsSync = true;
                break;
            }
        }
        if (needsSync && level.getGameTime() - lastSync >= NETWORK_UPDATE_RATE) {
            needsSync = false;
            lastSync = level.getGameTime();
            for (Section section : sections.values()) {
                section.lastSentAmount = section.amount;
                section.lastSentDirection = section.getCurrentDirection();
            }
            pipe.getHolder().scheduleNetworkUpdate();
        }
    }

    private void moveFromPipe() {
        for (EnumPipePart part : EnumPipePart.FACES) {
            Section section = sections.get(part);
            if (!section.getCurrentDirection().canOutput()) continue;
            int maxDrain = section.drainInternal(fluidTransferInfo.transferPerTick(), false);
            if (maxDrain <= 0) continue;
            PipeEventFluid.SideCheck sideCheck = new PipeEventFluid.SideCheck(pipe.getHolder(), this, currentFluid);
            sideCheck.disallowAllExcept(part.face);
            pipe.getHolder().fireEvent(sideCheck);
            if (sideCheck.getOrder().size() == 1) {
                if (!pipe.isConnected(part.face)) continue;
                IFluidHandlerBC fluidHandler = getNeighbourHandler(part.face);
                if (fluidHandler == null) continue;
                int filled = fluidHandler.fill(currentFluid.withAmount(maxDrain), false);
                if (filled > 0) {
                    section.drainInternal(filled, true);
                    section.ticksInDirection = COOLDOWN_OUTPUT;
                }
            }
        }
    }

    private void moveFromCenter() {
        Section center = sections.get(EnumPipePart.CENTER);
        int totalAvailable = center.getMaxDrained();
        if (totalAvailable < 1) {
            return;
        }
        int flowRate = fluidTransferInfo.transferPerTick();
        Set<Direction> realDirections = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            Section section = sections.get(EnumPipePart.fromFacing(direction));
            if (!section.getCurrentDirection().canOutput()) {
                continue;
            }
            if (section.getMaxFilled() > 0 && pipe.isConnected(direction)) {
                realDirections.add(direction);
            }
        }
        if (realDirections.isEmpty()) {
            return;
        }
        PipeEventFluid.SideCheck sideCheck = new PipeEventFluid.SideCheck(pipe.getHolder(), this, currentFluid);
        sideCheck.disallowAllExcept(realDirections);
        pipe.getHolder().fireEvent(sideCheck);
        List<Direction> random = new ArrayList<>(sideCheck.getOrder());
        Collections.shuffle(random);
        float min = Math.min(flowRate * realDirections.size(), totalAvailable) / (float) flowRate / realDirections.size();
        for (Direction direction : random) {
            Section section = sections.get(EnumPipePart.fromFacing(direction));
            int available = section.fill(flowRate, false);
            int amountToPush = Math.max(1, (int) (available * min));
            amountToPush = center.drainInternal(amountToPush, false);
            if (amountToPush > 0) {
                int filled = section.fill(amountToPush, true);
                if (filled > 0) {
                    center.drainInternal(filled, true);
                    section.ticksInDirection = COOLDOWN_OUTPUT;
                }
            }
        }
    }

    private void moveToCenter() {
        int transferInCount = 0;
        Section center = sections.get(EnumPipePart.CENTER);
        int spaceAvailable = capacity - center.amount;
        if (spaceAvailable <= 0 || center.getMaxFilled() <= 0) {
            return;
        }
        int flowRate = fluidTransferInfo.transferPerTick();
        List<EnumPipePart> faces = new ArrayList<>(Arrays.asList(EnumPipePart.FACES));
        Collections.shuffle(faces);
        int[] inputPerTick = new int[6];
        for (EnumPipePart part : faces) {
            Section section = sections.get(part);
            inputPerTick[part.getIndex()] = 0;
            if (section.getCurrentDirection().canInput()) {
                inputPerTick[part.getIndex()] = section.drainInternal(flowRate, false);
                if (inputPerTick[part.getIndex()] > 0) {
                    transferInCount++;
                }
            }
        }
        if (transferInCount == 0) {
            return;
        }
        int[] totalOffered = Arrays.copyOf(inputPerTick, 6);
        PipeEventFluid.PreMoveToCentre preMove = new PipeEventFluid.PreMoveToCentre(pipe.getHolder(), this, currentFluid,
            Math.min(flowRate, spaceAvailable), totalOffered, inputPerTick);
        pipe.getHolder().fireEvent(preMove);

        int[] fluidLeavingSide = new int[6];
        int left = Math.min(flowRate, spaceAvailable);
        float min = Math.min(flowRate * transferInCount, spaceAvailable) / (float) flowRate / transferInCount;
        for (EnumPipePart part : EnumPipePart.FACES) {
            Section section = sections.get(part);
            int i = part.getIndex();
            if (inputPerTick[i] > 0) {
                int amountToDrain = Math.max(1, (int) (inputPerTick[i] * min));
                amountToDrain = Math.min(amountToDrain, left);
                int amountToPush = section.drainInternal(amountToDrain, false);
                if (amountToPush > 0) {
                    fluidLeavingSide[i] = amountToPush;
                    left -= amountToPush;
                }
            }
        }

        int[] fluidEnteringCentre = Arrays.copyOf(fluidLeavingSide, 6);
        PipeEventFluid.OnMoveToCentre move = new PipeEventFluid.OnMoveToCentre(pipe.getHolder(), this, currentFluid,
            fluidLeavingSide, fluidEnteringCentre);
        pipe.getHolder().fireEvent(move);

        for (EnumPipePart part : EnumPipePart.FACES) {
            Section section = sections.get(part);
            int i = part.getIndex();
            int leaving = fluidLeavingSide[i];
            if (leaving > 0) {
                int actuallyDrained = section.drainInternal(leaving, true);
                if (actuallyDrained > 0) {
                    section.ticksInDirection = COOLDOWN_INPUT;
                }
                int entering = Math.min(fluidEnteringCentre[i], actuallyDrained);
                if (entering > 0) {
                    center.fill(entering, true);
                }
            }
        }
    }

    // Snapshots, so other mods' transactions can roll back fluid they put in

    private record Snapshot(BCFluidStack fluid, int[] amounts, int[] ticksInDirection, int[][] incoming, int[] incomingTotals) {}

    private Snapshot createSnapshot() {
        int n = EnumPipePart.VALUES.length;
        int[] amounts = new int[n], ticks = new int[n], totals = new int[n];
        int[][] incoming = new int[n][];
        for (EnumPipePart part : EnumPipePart.VALUES) {
            Section s = sections.get(part);
            int i = part.getIndex();
            amounts[i] = s.amount;
            ticks[i] = s.ticksInDirection;
            incoming[i] = s.incoming.clone();
            totals[i] = s.incomingTotalCache;
        }
        return new Snapshot(currentFluid, amounts, ticks, incoming, totals);
    }

    private void restoreSnapshot(Snapshot snapshot) {
        currentFluid = snapshot.fluid();
        for (EnumPipePart part : EnumPipePart.VALUES) {
            Section s = sections.get(part);
            int i = part.getIndex();
            s.amount = snapshot.amounts()[i];
            s.ticksInDirection = snapshot.ticksInDirection()[i];
            s.incoming = snapshot.incoming()[i];
            s.incomingTotalCache = snapshot.incomingTotals()[i];
        }
    }

    enum Dir {
        IN,
        NONE,
        OUT;

        boolean canInput() {
            return this != OUT;
        }

        boolean canOutput() {
            return this != IN;
        }
    }

    /** One of the seven parts of a pipe that fluid is held in. Other blocks fill the pipe through the sides. */
    class Section implements IFluidHandlerBC, ISnapshotable {
        final EnumPipePart part;
        int amount = 0;
        int lastSentAmount = 0;
        Dir lastSentDirection = Dir.NONE;
        int currentTime = 0;
        int[] incoming = new int[1];
        int incomingTotalCache = 0;
        int ticksInDirection = 0;
        int clientAmountThis, clientAmountLast;
        int target = 0;

        Section(EnumPipePart part) {
            this.part = part;
        }

        void save(ValueOutput output) {
            output.putInt("amount", amount);
            output.putInt("ticksInDirection", ticksInDirection);
            output.putIntArray("incoming", incoming);
        }

        void load(ValueInput input) {
            amount = input.getIntOr("amount", 0);
            ticksInDirection = input.getIntOr("ticksInDirection", 0);
            int[] in = input.getIntArray("incoming").orElse(new int[0]);
            incomingTotalCache = 0;
            for (int i = 0; i < incoming.length; i++) {
                incoming[i] = i < in.length ? in[i] : 0;
                incomingTotalCache += incoming[i];
            }
        }

        int getMaxFilled() {
            int availableTotal = capacity - amount;
            int availableThisTick = fluidTransferInfo.transferPerTick() - incoming[currentTime];
            return Math.min(availableTotal, availableThisTick);
        }

        int getMaxDrained() {
            return Math.min(amount - incomingTotalCache, fluidTransferInfo.transferPerTick());
        }

        int fill(int maxFill, boolean doFill) {
            int amountToFill = Math.min(getMaxFilled(), maxFill);
            if (amountToFill <= 0) {
                return 0;
            }
            if (doFill) {
                incoming[currentTime] += amountToFill;
                incomingTotalCache += amountToFill;
                amount += amountToFill;
            }
            return amountToFill;
        }

        int fillInternal(int maxFill, boolean doFill) {
            int amountToFill = Math.min(capacity - amount, maxFill);
            if (amountToFill <= 0) {
                return 0;
            }
            if (doFill) {
                incoming[currentTime] += amountToFill;
                incomingTotalCache += amountToFill;
                amount += amountToFill;
            }
            return amountToFill;
        }

        int drainInternal(int maxDrain, boolean doDrain) {
            maxDrain = Math.min(maxDrain, getMaxDrained());
            if (maxDrain <= 0) {
                return 0;
            }
            if (doDrain) {
                amount -= maxDrain;
            }
            return maxDrain;
        }

        void advanceForMovement() {
            incomingTotalCache -= incoming[currentTime];
            incoming[currentTime] = 0;
        }

        Dir getCurrentDirection() {
            return ticksInDirection == 0 ? Dir.NONE : ticksInDirection < 0 ? Dir.IN : Dir.OUT;
        }

        void tickClient() {
            clientAmountLast = clientAmountThis;
            if (target != clientAmountThis) {
                int delta = target - clientAmountThis;
                if (Math.abs(delta) < NETWORK_UPDATE_RATE) {
                    clientAmountThis += delta;
                } else {
                    clientAmountThis += delta / NETWORK_UPDATE_RATE;
                }
            }
        }

        // IFluidHandlerBC: other blocks can only fill the pipe

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public BCFluidStack getFluidInTank(int tank) {
            return currentFluid.isEmpty() ? BCFluidStack.EMPTY : currentFluid.withAmount(amount);
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, BCFluidStack stack) {
            return currentFluid.isEmpty() || currentFluid.isSameFluid(stack);
        }

        @Override
        public int fill(BCFluidStack resource, boolean simulate) {
            if (part.face == null || resource.isEmpty() || !getCurrentDirection().canInput() || !pipe.isConnected(part.face)) {
                return 0;
            }
            PipeEventFluid.TryInsert tryInsert = new PipeEventFluid.TryInsert(pipe.getHolder(), PipeFlowFluids.this, part.face,
                resource);
            pipe.getHolder().fireEvent(tryInsert);
            if (tryInsert.isCanceled()) {
                return 0;
            }
            if (currentFluid.isEmpty() || currentFluid.isSameFluid(resource)) {
                if (!simulate && currentFluid.isEmpty()) {
                    setFluid(resource);
                }
                int filled = fill(resource.getAmount(), !simulate);
                if (filled > 0 && !simulate) {
                    ticksInDirection = COOLDOWN_INPUT;
                }
                return filled;
            }
            return 0;
        }

        @Override
        public BCFluidStack drain(BCFluidStack resource, boolean simulate) {
            return BCFluidStack.EMPTY;
        }

        @Override
        public BCFluidStack drain(Predicate<BCFluidStack> filter, int maxDrain, boolean simulate) {
            return BCFluidStack.EMPTY;
        }

        @Override
        public Object createSnapshot() {
            return PipeFlowFluids.this.createSnapshot();
        }

        @Override
        public void restoreSnapshot(Object snapshot) {
            PipeFlowFluids.this.restoreSnapshot((Snapshot) snapshot);
        }

        @Override
        public void onSnapshotCommit() {}
    }
}
