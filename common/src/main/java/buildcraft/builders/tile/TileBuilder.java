/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.IPathProvider;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IHasWork;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BuildEngine;
import buildcraft.builders.block.BlockFacing;
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.builders.snapshot.SnapshotStore;
import buildcraft.lib.inventory.IContainerDelegate;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.lib.misc.ChunkLoader;
import buildcraft.lib.tile.TileBC;

/** Builds the template or blueprint in its slot, in front of it (where the architect table's area was, relative to the
 * table), using the blocks in its inventory. If it's placed next to a path of path markers, it builds the snapshot at
 * every block along the path instead, one after another. */
public class TileBuilder extends TileBC implements MenuProvider, IMjConnectorProvider, IHasWork, IControllable, IHasBuildBox, IContainerDelegate {
    private final ChunkLoader chunkLoader = new ChunkLoader();
    public static final int INV_SIZE = 27;

    public final ItemHandlerSimple invSnapshot = new ItemHandlerSimple(1,
        (slot, stack) -> stack.getItem() instanceof ItemSnapshot && ItemSnapshot.getHeader(stack) != null, this::onSnapshotChanged);
    public final ItemHandlerSimple inv = new ItemHandlerSimple(INV_SIZE, this::onInventoryChanged);
    private final MjBattery battery = new MjBattery(16000 * MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);
    private Mode mode = Mode.UNKNOWN;

    @Nullable
    private BuildEngine engine;
    /** The area being built. Synced to the client for drawing. */
    @Nullable
    private BoundingBox box;
    private boolean needsRebuild = true;
    /** Every block along the path being built along (or null to build in front), and how far along it the builder is. */
    @Nullable
    private List<BlockPos> path;
    private int pathIndex = 0;

    public TileBuilder(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.BUILDER_TILE.get(), pos, state);
    }

    @Override
    @Nullable
    public BoundingBox getBox() {
        return box;
    }

    public int getLeftToBreak() {
        return engine == null ? 0 : engine.getLeftToBreak();
    }

    public int getLeftToPlace() {
        return engine == null ? 0 : engine.getLeftToPlace();
    }

    public boolean isFinished() {
        return engine != null && engine.isFinished(true) && (path == null || pathIndex >= path.size() - 1);
    }

    public long getStoredPower() {
        return battery.getStored();
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    private void onSnapshotChanged() {
        needsRebuild = true;
        setChanged();
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level == null || level.isClientSide()) return;
        Direction facing = getBlockState().getValue(BlockFacing.FACING);
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof IPathProvider provider) {
            List<BlockPos> markers = provider.getPath();
            if (markers.size() >= 2) {
                path = allAlong(markers);
                pathIndex = 0;
                provider.removeFromWorld();
                needsRebuild = true;
                setChanged();
            }
        }
    }

    /** @return Every block on the straight lines between the given positions, in order. */
    private static List<BlockPos> allAlong(List<BlockPos> markers) {
        List<BlockPos> all = new ArrayList<>();
        all.add(markers.getFirst());
        for (int i = 1; i < markers.size(); i++) {
            BlockPos from = markers.get(i - 1), to = markers.get(i);
            int steps = Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ())));
            for (int s = 1; s <= steps; s++) {
                double t = s / (double) steps;
                all.add(BlockPos.containing(from.getX() + 0.5 + (to.getX() - from.getX()) * t, from.getY() + 0.5 + (to.getY() - from.getY()) * t,
                    from.getZ() + 0.5 + (to.getZ() - from.getZ()) * t));
            }
        }
        return all;
    }

    /** @return Where the builder would be for the snapshot to be built at the current step (the snapshot is built in front
     *         of this position). */
    private BlockPos getOrigin(Direction facing) {
        if (path == null || path.isEmpty()) return worldPosition;
        return path.get(Math.min(pathIndex, path.size() - 1)).relative(facing);
    }

    private void onInventoryChanged() {
        if (engine != null) engine.onResourcesChanged();
        setChanged();
    }

    /** Loads the snapshot in the slot and works out what to build. */
    private void rebuild() {
        needsRebuild = false;
        engine = null;
        BoundingBox oldBox = box;
        box = null;
        if (!(level instanceof ServerLevel serverLevel)) return;
        SnapshotHeader header = ItemSnapshot.getHeader(invSnapshot.getItem(0));
        Snapshot snapshot = header == null ? null : SnapshotStore.get(serverLevel.getServer(), header.key());
        if (snapshot != null) {
            Direction facing = getBlockState().getValue(BlockFacing.FACING);
            BlockPos origin = getOrigin(facing);
            BoundingBox area = snapshot.getBox(origin, facing);
            box = area;
            engine = new BuildEngine(serverLevel, worldPosition, area, createPlan(snapshot, area, facing, origin));
        }
        if (oldBox == null ? box != null : !oldBox.equals(box)) {
            sendNetworkUpdate();
        }
    }

    private BuildEngine.Plan createPlan(Snapshot snapshot, BoundingBox area, Direction facing, BlockPos origin) {
        Rotation rotation = snapshot.rotationTo(facing);
        int sizeX = area.getXSpan(), sizeZ = area.getZSpan();
        int count = sizeX * area.getYSpan() * sizeZ;
        BlockState[] wanted = new BlockState[count];
        boolean[] skipped = new boolean[count];
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(area.minX() + i % sizeX, area.minY() + i / (sizeX * sizeZ), area.minZ() + (i / sizeX) % sizeZ);
            int index = snapshot.indexOfWorldPos(pos, origin, facing);
            BlockState state = index < 0 ? air : snapshot.getState(index);
            if (state != null && !state.isAir()) {
                state = state.rotate(rotation);
                skipped[i] = isPlacedWithOtherPart(state) || state.getBlock().asItem() == Items.AIR;
            }
            wanted[i] = state;
        }
        return new BuildEngine.Plan() {
            @Override
            public @Nullable BlockState getWanted(int index) {
                return wanted[index];
            }

            @Override
            public boolean isSkipped(int index) {
                return skipped[index];
            }
        };
    }

    /** @return True for the parts of blocks (such as the top of a door) that are placed along with another part. */
    private static boolean isPlacedWithOtherPart(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (needsRebuild) rebuild();
        if (level instanceof ServerLevel serverLevel) {
            chunkLoader.tick(serverLevel, ChunkLoader.chunksFor(worldPosition, box), BCBuildersConfig.chunkLoadMachines);
        }
        if (engine != null) {
            engine.tick(battery, inv, true, mode != Mode.OFF);
            if (engine.isFinished(true) && path != null && pathIndex < path.size() - 1) {
                pathIndex++;
                setChanged();
                rebuild();
            }
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        needsRebuild = true;
    }

    // IHasWork

    @Override
    public boolean hasWork() {
        return mode != Mode.OFF && engine != null && !isFinished();
    }

    // IControllable

    @Override
    public Mode getControlMode() {
        return mode;
    }

    @Override
    public void setControlMode(Mode mode) {
        this.mode = mode;
        setChanged();
    }

    @Override
    public boolean acceptsControlMode(Mode mode) {
        return mode == Mode.ON || mode == Mode.OFF;
    }

    // Saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invSnapshot.save(output, "snapshot");
        inv.save(output, "inv");
        battery.save(output, "battery");
        output.putString("mode", mode.name());
        if (box != null) {
            output.putIntArray("box", new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
        }
        if (path != null) {
            output.store("path", BlockPos.CODEC.listOf(), path);
            output.putInt("pathIndex", pathIndex);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invSnapshot.load(input, "snapshot");
        inv.load(input, "inv");
        battery.load(input, "battery");
        try {
            mode = Mode.valueOf(input.getStringOr("mode", "UNKNOWN"));
        } catch (IllegalArgumentException e) {
            mode = Mode.UNKNOWN;
        }
        box = input.getIntArray("box").filter(a -> a.length == 6)
            .map(a -> new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5])).orElse(null);
        path = input.read("path", BlockPos.CODEC.listOf()).map(list -> (List<BlockPos>) new ArrayList<>(list)).orElse(null);
        pathIndex = input.getIntOr("pathIndex", 0);
        needsRebuild = true;
    }

    // GUI

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerBuilder(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, invSnapshot);
        }
        if (level instanceof ServerLevel serverLevel) chunkLoader.releaseAll(serverLevel);
    }

    @Override
    public Container getDelegate() {
        return inv;
    }
}
