/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.api.tiles.IAreaProvider;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.lib.misc.ChunkLoader;
import buildcraft.lib.tile.TileBC;

/** The quarry builds a frame around its area, then moves a drill over it, mining it out layer by layer. */
public class TileQuarry extends TileBC implements IMjConnectorProvider, IHasWork, IControllable {
    private final ChunkLoader chunkLoader = new ChunkLoader();
    private static final long MAX_POWER_PER_TICK = 512 * MjAPI.MJ;
    private static final long FRAME_POWER = 24 * MjAPI.MJ;
    private static final int MAX_SCAN_PER_TICK = 4096;
    /** How long a finished quarry waits before looking over its area again. */
    private static final int RESCAN_DELAY = 400;

    private final MjBattery battery = new MjBattery(24000 * MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);

    @Nullable
    private BoundingBox frameBox;
    @Nullable
    private BoundingBox miningBox;
    /** The frame positions in the order they are built: outwards from the quarry. */
    private final List<BlockPos> framePoses = new ArrayList<>();
    /** Every position in the frame box, checked a few at a time to see if it needs work. */
    private final Deque<BlockPos> toCheck = new ArrayDeque<>();
    private final Set<BlockPos> firstCheckedPoses = new HashSet<>();
    private boolean firstChecked = false;
    private final Set<BlockPos> frameBreakPoses = new TreeSet<>(
        Comparator.<BlockPos> comparingDouble(p -> p.distSqr(worldPosition)).thenComparing(Comparator.naturalOrder()));
    private final Set<BlockPos> framePlacePoses = new HashSet<>();

    /** The index of the current block in the mining area, in mining order. */
    private long mineIndex = 0;
    @Nullable
    private Task currentTask;
    /** Where the drill is (the corner of the block it's at), or null while the frame is being built. */
    @Nullable
    private Vec3 drillPos;

    // Per tick limits
    private double blockPercentSoFar;
    private double moveDistanceSoFar;

    // Client side
    @Nullable
    public Vec3 clientDrillPos, prevClientDrillPos;
    private int syncCooldown = 0;
    private int rescanTimer = 0;
    private boolean needsSync = false;

    public TileQuarry(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.QUARRY_TILE.get(), pos, state);
    }

    @Nullable
    public BoundingBox getFrameBox() {
        return frameBox;
    }

    @Nullable
    public Vec3 getDrillPos() {
        return drillPos;
    }

    @Nullable
    public Task getCurrentTask() {
        return currentTask;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    // Setup

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level != null && !level.isClientSide()) {
            setupArea();
        }
    }

    /** Works out the area to mine: from the markers behind the quarry, or a default area. */
    private void setupArea() {
        if (level == null) return;
        Direction facing = getBlockState().getValue(BlockQuarry.FACING);
        BlockPos areaPos = worldPosition.relative(facing.getOpposite());
        BlockPos min = null, max = null;
        if (level.getBlockEntity(areaPos) instanceof IAreaProvider provider) {
            BlockPos pMin = provider.min(), pMax = provider.max();
            if (pMax.getX() - pMin.getX() >= 3 && pMax.getZ() - pMin.getZ() >= 3) {
                min = pMin;
                max = pMax;
                provider.removeFromWorld();
            }
        }
        if (min == null) {
            BlockPos p = worldPosition;
            switch (facing.getOpposite()) {
                case WEST -> {
                    min = p.offset(-11, 0, -5);
                    max = p.offset(-1, 4, 5);
                }
                case SOUTH -> {
                    min = p.offset(-5, 0, 1);
                    max = p.offset(5, 4, 11);
                }
                case NORTH -> {
                    min = p.offset(-5, 0, -11);
                    max = p.offset(5, 4, -1);
                }
                default -> {
                    min = p.offset(1, 0, -5);
                    max = p.offset(11, 4, 5);
                }
            }
        }
        if (max.getY() - min.getY() < BCBuildersConfig.quarryFrameMinHeight) {
            max = new BlockPos(max.getX(), min.getY() + BCBuildersConfig.quarryFrameMinHeight, max.getZ());
        }
        if (max.getY() > level.getMaxY()) {
            int dist = max.getY() - level.getMaxY();
            min = min.below(dist);
            max = max.below(dist);
        }
        frameBox = BoundingBox.fromCorners(min, max);
        int minY = Math.max(level.getMinY(), max.getY() - 1 - BlockUtil.MINING_MAX_DEPTH);
        miningBox = new BoundingBox(min.getX() + 1, minY, min.getZ() + 1, max.getX() - 1, max.getY() - 1, max.getZ() - 1);
        mineIndex = 0;
        drillPos = null;
        currentTask = null;
        updatePoses();
        setChanged();
        sendNetworkUpdate();
    }

    private static boolean isOnEdge(BoundingBox box, BlockPos p) {
        if (!box.isInside(p)) return false;
        int count = 0;
        if (p.getX() == box.minX() || p.getX() == box.maxX()) count++;
        if (p.getY() == box.minY() || p.getY() == box.maxY()) count++;
        if (p.getZ() == box.minZ() || p.getZ() == box.maxZ()) count++;
        return count >= 2;
    }

    private void updatePoses() {
        framePoses.clear();
        toCheck.clear();
        firstCheckedPoses.clear();
        firstChecked = false;
        frameBreakPoses.clear();
        framePlacePoses.clear();
        if (frameBox == null) return;
        List<BlockPos> all = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(frameBox.minX(), frameBox.minY(), frameBox.minZ(), frameBox.maxX(), frameBox.maxY(),
            frameBox.maxZ())) {
            all.add(p.immutable());
        }
        all.sort(Comparator.<BlockPos> comparingDouble(p -> p.distSqr(worldPosition)).thenComparing(Comparator.naturalOrder()));
        toCheck.addAll(all);
        // The frame is built outwards from the quarry, following the edges
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(worldPosition);
        while (!open.isEmpty()) {
            BlockPos p = open.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos next = p.relative(dir);
                if (isOnEdge(frameBox, next) && visited.add(next)) {
                    framePoses.add(next);
                    open.add(next);
                }
            }
        }
    }

    // Block checks

    private boolean isWaterLike(FluidState fluid) {
        return !fluid.isEmpty() && !fluid.is(FluidTags.LAVA);
    }

    private boolean canMine(BlockPos p) {
        if (level == null) return false;
        BlockState state = level.getBlockState(p);
        if (state.getDestroySpeed(level, p) < 0) return false;
        FluidState fluid = level.getFluidState(p);
        return fluid.isEmpty() || isWaterLike(fluid);
    }

    private boolean canMoveThrough(BlockPos p) {
        if (level == null) return false;
        BlockState state = level.getBlockState(p);
        if (state.isAir()) return true;
        return state.liquid() && isWaterLike(level.getFluidState(p));
    }

    private boolean canMoveDownTo(BlockPos p) {
        if (miningBox == null) return false;
        for (int y = miningBox.maxY(); y > p.getY(); y--) {
            if (!canMoveThrough(new BlockPos(p.getX(), y, p.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /** @return True for solid blocks: the ones that are in the frame's way. */
    private boolean isSolid(BlockPos p) {
        return level != null && !level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty();
    }

    private void check(BlockPos p) {
        if (level == null || frameBox == null) return;
        frameBreakPoses.remove(p);
        framePlacePoses.remove(p);
        BlockState state = level.getBlockState(p);
        if (isOnEdge(frameBox, p)) {
            if (!state.is(BCBuildersBlocks.FRAME.get())) {
                if (isSolid(p)) {
                    frameBreakPoses.add(p);
                } else {
                    framePlacePoses.add(p);
                }
            }
        } else if (isSolid(p) && !p.equals(worldPosition)) {
            frameBreakPoses.add(p);
        }
        if (!firstChecked) {
            firstCheckedPoses.add(p);
            if (firstCheckedPoses.size() >= toCheck.size()) {
                firstChecked = true;
                firstCheckedPoses.clear();
            }
        }
    }

    // Mining order: top down, a snaking line across each layer

    private long miningVolume() {
        if (miningBox == null) return 0;
        return (long) miningBox.getXSpan() * miningBox.getYSpan() * miningBox.getZSpan();
    }

    private BlockPos minePos(long index) {
        BoundingBox box = miningBox;
        int w = box.getXSpan(), d = box.getZSpan();
        long layerSize = (long) w * d;
        int layer = (int) (index / layerSize);
        int inLayer = (int) (index % layerSize);
        if ((layer & 1) == 1) {
            inLayer = (int) (layerSize - 1 - inLayer);
        }
        int row = inLayer / w;
        int col = inLayer % w;
        if ((row & 1) == 1) {
            col = w - 1 - col;
        }
        return new BlockPos(box.minX() + col, box.maxY() - layer, box.minZ() + row);
    }

    private boolean isMineTarget(BlockPos p) {
        return !canMoveThrough(p) && canMine(p) && canMoveDownTo(p);
    }

    /** @return The next block to mine, or null if none was found this tick (either because the quarry has finished,
     *         or because it looked at too many empty positions). */
    @Nullable
    private BlockPos nextMinePos() {
        long volume = miningVolume();
        for (int i = 0; i < MAX_SCAN_PER_TICK && mineIndex < volume; i++) {
            BlockPos p = minePos(mineIndex);
            if (isMineTarget(p)) return p;
            mineIndex++;
        }
        return null;
    }

    public boolean isFinished() {
        return miningBox != null && mineIndex >= miningVolume();
    }

    // Ticking

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide()) {
            prevClientDrillPos = clientDrillPos;
            clientDrillPos = drillPos;
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            chunkLoader.tick(serverLevel, ChunkLoader.chunksFor(worldPosition, frameBox), BCBuildersConfig.chunkLoadMachines);
        }
        if (frameBox == null || miningBox == null) {
            // Placed without a player (such as by a command), so it never looked for its area
            setupArea();
            if (frameBox == null || miningBox == null) return;
        }
        if (framePoses.isEmpty() && toCheck.isEmpty()) {
            updatePoses();
        }
        battery.tick();
        if (controlMode == IControllable.Mode.OFF) return;

        int checks = firstChecked ? 10 : 500;
        for (int i = 0; i < checks && !toCheck.isEmpty(); i++) {
            BlockPos p = toCheck.pollFirst();
            check(p);
            toCheck.addLast(p);
        }
        if (!firstChecked) return;

        long max;
        if (battery.getStored() > battery.getCapacity() / 2) {
            max = MAX_POWER_PER_TICK;
        } else {
            double fraction = (battery.getStored() + MjAPI.MJ / 2) / (double) (battery.getCapacity() / 2);
            max = Math.max(0, Math.min(MAX_POWER_PER_TICK, (long) (MAX_POWER_PER_TICK * fraction)));
        }
        blockPercentSoFar = 0;
        moveDistanceSoFar = 0;
        int maxTasks = Math.max(1, (int) (max * BCBuildersConfig.quarryMaxTasksPerTick / MAX_POWER_PER_TICK));
        boolean changed = false;
        taskLoop:
        for (int i = 0; i < maxTasks; i++) {
            if (currentTask != null) {
                long needed = currentTask.getRequiredPowerThisTick();
                int div = BCBuildersConfig.quarryTaskPowerDivisor;
                long added;
                if (div > 0) {
                    long power = battery.extractPower(0, Math.min(max, needed * (div + i) / div));
                    max -= power;
                    added = power * div / (div + i);
                } else {
                    added = battery.extractPower(0, Math.min(max, needed));
                    max -= added;
                }
                changed = true;
                if (currentTask.addPower(added)) {
                    currentTask = null;
                } else {
                    break;
                }
            }
            if (!frameBreakPoses.isEmpty()) {
                BlockPos p = frameBreakPoses.iterator().next();
                if (canMine(p)) {
                    drillPos = null;
                    currentTask = new TaskBreakBlock(p);
                    changed = true;
                }
                check(p);
                continue;
            }
            if (!framePlacePoses.isEmpty()) {
                for (BlockPos p : framePoses) {
                    if (!framePlacePoses.contains(p)) continue;
                    check(p);
                    if (!framePlacePoses.contains(p)) continue;
                    drillPos = null;
                    currentTask = new TaskAddFrame(p);
                    changed = true;
                    continue taskLoop;
                }
            }
            if (drillPos == null) {
                // The frame is done: start (or restart) mining from the top
                mineIndex = 0;
                drillPos = Vec3.atLowerCornerOf(new BlockPos(
                    clamp(worldPosition.getX(), miningBox.minX(), miningBox.maxX()), miningBox.maxY(),
                    clamp(worldPosition.getZ(), miningBox.minZ(), miningBox.maxZ())));
                changed = true;
            }
            BlockPos target = nextMinePos();
            if (target == null) {
                if (isFinished() && ++rescanTimer >= RESCAN_DELAY) {
                    rescanTimer = 0;
                    mineIndex = 0;
                }
                break;
            }
            Vec3 targetVec = Vec3.atLowerCornerOf(target);
            if (drillPos.distanceToSqr(targetVec) >= 1) {
                currentTask = new TaskMoveDrill(drillPos, targetVec);
            } else {
                currentTask = new TaskBreakBlock(target);
            }
            changed = true;
        }
        if (changed) {
            setChanged();
            needsSync = true;
        }
        if (needsSync && --syncCooldown <= 0) {
            needsSync = false;
            syncCooldown = 2;
            sendNetworkUpdate();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) chunkLoader.releaseAll(serverLevel);
        if (currentTask instanceof TaskBreakBlock task) {
            level.destroyBlockProgress(task.breakPos.hashCode(), task.breakPos, -1);
        }
        if (frameBox != null) {
            if (framePoses.isEmpty()) updatePoses();
            for (BlockPos p : framePoses) {
                if (level.getBlockState(p).is(BCBuildersBlocks.FRAME.get())) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // Saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (frameBox != null && miningBox != null) {
            output.store("frame", BoundingBox.CODEC, frameBox);
            output.store("mining", BoundingBox.CODEC, miningBox);
        }
        battery.save(output, "battery");
        output.putLong("mineIndex", mineIndex);
        if (drillPos != null) {
            output.store("drill", Vec3.CODEC, drillPos);
        }
        if (currentTask != null) {
            ValueOutput task = output.child("task");
            currentTask.save(task);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        frameBox = input.read("frame", BoundingBox.CODEC).orElse(null);
        miningBox = input.read("mining", BoundingBox.CODEC).orElse(null);
        battery.load(input, "battery");
        mineIndex = input.getLongOr("mineIndex", 0);
        drillPos = input.read("drill", Vec3.CODEC).orElse(null);
        currentTask = input.child("task").map(this::loadTask).orElse(null);
        framePoses.clear();
        toCheck.clear();
    }

    @Nullable
    private Task loadTask(ValueInput input) {
        String type = input.getStringOr("type", "");
        long power = input.getLongOr("power", 0);
        Task task = switch (type) {
            case "break" -> input.read("pos", BlockPos.CODEC).map(TaskBreakBlock::new).orElse(null);
            case "frame" -> input.read("pos", BlockPos.CODEC).map(TaskAddFrame::new).orElse(null);
            case "move" -> {
                Vec3 from = input.read("from", Vec3.CODEC).orElse(null);
                Vec3 to = input.read("to", Vec3.CODEC).orElse(null);
                yield from == null || to == null ? null : new TaskMoveDrill(from, to);
            }
            default -> null;
        };
        if (task != null) {
            task.power = Math.max(0, power);
        }
        return task;
    }

    // Tasks

    public abstract class Task {
        public long power;

        public abstract long getTarget();

        public long getRequiredPowerThisTick() {
            return Math.max(0, getTarget() - power);
        }

        /** @return True if the task is finished or cancelled. */
        protected abstract boolean onReceivePower(long added, long target);

        /** @return False if the task couldn't be finished, so its power should be refunded. */
        protected abstract boolean finish(long added, long target);

        /** @return True if this task has been completed, or cancelled. */
        final boolean addPower(long microJoules) {
            power += microJoules;
            long target = getTarget();
            if (power >= target) {
                if (!finish(microJoules, target)) {
                    battery.addPower(Math.min(power, battery.getCapacity() - battery.getStored()), false);
                }
                return true;
            }
            return onReceivePower(microJoules, target);
        }

        void save(ValueOutput output) {
            output.putLong("power", power);
        }
    }

    public class TaskBreakBlock extends Task {
        public final BlockPos breakPos;

        TaskBreakBlock(BlockPos pos) {
            this.breakPos = pos;
        }

        @Override
        public long getTarget() {
            return level == null ? 0 : BlockUtil.computeBlockBreakPower(level, breakPos);
        }

        @Override
        public long getRequiredPowerThisTick() {
            long target = getTarget();
            long req = Math.max(0, target - power);
            double rate = BCBuildersConfig.quarryMaxBlockMineRate;
            if (rate < 0.1) return req;
            rate = rate / 20 - blockPercentSoFar;
            if (rate <= 0) return 0;
            return Math.min(req, (long) (target * rate));
        }

        @Override
        protected boolean onReceivePower(long added, long target) {
            blockPercentSoFar += added / (double) target;
            if (level != null && !level.getBlockState(breakPos).isAir()) {
                level.destroyBlockProgress(breakPos.hashCode(), breakPos, (int) (power * 9 / target));
                return false;
            }
            return true;
        }

        @Override
        protected boolean finish(long added, long target) {
            blockPercentSoFar += added / (double) target;
            if (!(level instanceof ServerLevel server) || !canMine(breakPos)) {
                return true;
            }
            server.destroyBlockProgress(breakPos.hashCode(), breakPos, -1);
            List<ItemStack> drops = BlockUtil.breakBlockAndGetDrops(server, breakPos);
            if (drops != null) {
                drops.forEach(stack -> InventoryUtil.addToBestAcceptor(server, worldPosition, stack));
            }
            check(breakPos);
            return drops != null;
        }

        @Override
        void save(ValueOutput output) {
            super.save(output);
            output.putString("type", "break");
            output.store("pos", BlockPos.CODEC, breakPos);
        }
    }

    public class TaskAddFrame extends Task {
        public final BlockPos framePos;

        TaskAddFrame(BlockPos pos) {
            this.framePos = pos;
        }

        @Override
        public long getTarget() {
            return FRAME_POWER;
        }

        @Override
        protected boolean onReceivePower(long added, long target) {
            return isSolid(framePos);
        }

        @Override
        protected boolean finish(long added, long target) {
            if (level == null || isSolid(framePos)) {
                return false;
            }
            level.setBlock(framePos, BCBuildersBlocks.FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            // Let the new frame connect to the ones around it
            level.setBlock(framePos, Block.updateFromNeighbourShapes(level.getBlockState(framePos), level, framePos), Block.UPDATE_ALL);
            return true;
        }

        @Override
        void save(ValueOutput output) {
            super.save(output);
            output.putString("type", "frame");
            output.store("pos", BlockPos.CODEC, framePos);
        }
    }

    public class TaskMoveDrill extends Task {
        public final Vec3 from, to;

        TaskMoveDrill(Vec3 from, Vec3 to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public long getTarget() {
            return (long) (from.distanceTo(to) * 20 * MjAPI.MJ);
        }

        @Override
        public long getRequiredPowerThisTick() {
            long req = Math.max(0, getTarget() - power);
            double max = BCBuildersConfig.quarryMaxFrameMoveSpeed;
            if (max < 0.1) return req;
            max = max / 20 - moveDistanceSoFar;
            if (max <= 0) return 0;
            return Math.min(req, (long) (max * 20 * MjAPI.MJ));
        }

        @Override
        protected boolean onReceivePower(long added, long target) {
            moveDistanceSoFar += added / (double) MjAPI.MJ / 20;
            double progress = power / (double) target;
            drillPos = from.scale(1 - progress).add(to.scale(progress));
            return false;
        }

        @Override
        protected boolean finish(long added, long target) {
            moveDistanceSoFar += added / (double) MjAPI.MJ / 20;
            drillPos = to;
            return true;
        }

        @Override
        void save(ValueOutput output) {
            super.save(output);
            output.putString("type", "move");
            output.store("from", Vec3.CODEC, from);
            output.store("to", Vec3.CODEC, to);
        }
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
        return frameBox != null && (currentTask != null || !isFinished());
    }
}
