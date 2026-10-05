/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.tiles.IAreaProvider;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersComponents;
import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.container.ContainerArchitectTable;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.builders.snapshot.SnapshotStore;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.tile.TileBC;

/** Scans the area marked out behind it into a blank template or blueprint. */
public class TileArchitectTable extends TileBC implements MenuProvider, IHasBuildBox {
    private static final int TEMPLATE_PER_TICK = 1024;
    private static final int BLUEPRINT_PER_TICK = 256;

    public final ItemHandlerSimple invIn = new ItemHandlerSimple(1,
        (slot, stack) -> stack.getItem() instanceof ItemSnapshot && ItemSnapshot.getHeader(stack) == null, this::setChanged);
    public final ItemHandlerSimple invOut = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);

    @Nullable
    private BoundingBox box;
    /** How far through the scan this is, or -1 if not scanning. */
    private int scanIndex = -1;
    private Snapshot.@Nullable Type scanType;
    private final BitSet scannedFilled = new BitSet();
    private final List<BlockState> scannedPalette = new ArrayList<>();
    private final Map<BlockState, Integer> paletteIndex = new HashMap<>();
    private int[] scannedData = new int[0];

    public TileArchitectTable(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.ARCHITECT_TILE.get(), pos, state);
    }

    @Override
    @Nullable
    public BoundingBox getBox() {
        return box;
    }

    /** @return How much of the scan is done, from 0 to 1000, or -1 if not scanning. */
    public int getProgress() {
        if (scanIndex < 0 || box == null) return -1;
        return (int) (scanIndex * 1000L / volume(box));
    }

    private static int volume(BoundingBox box) {
        return box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level == null || level.isClientSide()) return;
        Direction facing = getBlockState().getValue(BlockArchitectTable.FACING);
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof IAreaProvider provider) {
            box = BoundingBox.fromCorners(provider.min(), provider.max());
            provider.removeFromWorld();
        }
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockArchitectTable.VALID, box != null));
        sendNetworkUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || box == null) return;
        ItemStack in = invIn.getItem(0);
        boolean canScan = !in.isEmpty() && invOut.getItem(0).isEmpty() && in.getItem() instanceof ItemSnapshot;
        if (!canScan) {
            if (scanIndex >= 0) resetScan();
            return;
        }
        Snapshot.Type type = ((ItemSnapshot) in.getItem()).type;
        if (scanIndex < 0 || scanType != type) {
            resetScan();
            scanIndex = 0;
            scanType = type;
            if (type == Snapshot.Type.BLUEPRINT) {
                scannedData = new int[volume(box)];
                addToPalette(Blocks.AIR.defaultBlockState());
            }
        }
        int total = volume(box);
        int sizeX = box.getXSpan(), sizeZ = box.getZSpan();
        int perTick = type == Snapshot.Type.TEMPLATE ? TEMPLATE_PER_TICK : BLUEPRINT_PER_TICK;
        for (int n = 0; n < perTick && scanIndex < total; n++, scanIndex++) {
            int x = scanIndex % sizeX, z = (scanIndex / sizeX) % sizeZ, y = scanIndex / (sizeX * sizeZ);
            BlockPos pos = new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
            BlockState state = level.getBlockState(pos);
            if (type == Snapshot.Type.TEMPLATE) {
                if (!state.isAir() && state.getFluidState().isEmpty()) scannedFilled.set(scanIndex);
            } else {
                // Fluids and things without an item (such as fire) can't be built, so they are left out
                if (!state.getFluidState().isEmpty() && state.canBeReplaced()) state = Blocks.AIR.defaultBlockState();
                scannedData[scanIndex] = addToPalette(state);
            }
        }
        if (scanIndex >= total) {
            finishScan(type);
        }
    }

    private int addToPalette(BlockState state) {
        return paletteIndex.computeIfAbsent(state, s -> {
            scannedPalette.add(s);
            return scannedPalette.size() - 1;
        });
    }

    private void resetScan() {
        scanIndex = -1;
        scanType = null;
        scannedFilled.clear();
        scannedPalette.clear();
        paletteIndex.clear();
        scannedData = new int[0];
    }

    private void finishScan(Snapshot.Type type) {
        Direction facing = getBlockState().getValue(BlockArchitectTable.FACING);
        BlockPos base = worldPosition.relative(facing.getOpposite());
        BlockPos offset = new BlockPos(box.minX(), box.minY(), box.minZ()).subtract(base);
        Snapshot snapshot = type == Snapshot.Type.TEMPLATE
            ? Snapshot.template(box.getXSpan(), box.getYSpan(), box.getZSpan(), facing, offset, (BitSet) scannedFilled.clone())
            : Snapshot.blueprint(box.getXSpan(), box.getYSpan(), box.getZSpan(), facing, offset, scannedPalette, scannedData.clone());
        UUID key = SnapshotStore.add(level.getServer(), snapshot);
        ItemStack in = invIn.getItem(0);
        ItemStack out = in.copyWithCount(1);
        String name = hasCustomName() ? getCustomName().getString() : "";
        out.set(BCBuildersComponents.SNAPSHOT.get(), SnapshotHeader.of(key, snapshot, name));
        in.shrink(1);
        invIn.setChanged();
        invOut.setItem(0, out);
        resetScan();
    }

    private boolean hasCustomName() {
        return components().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }

    @Nullable
    private Component getCustomName() {
        return components().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invIn.save(output, "in");
        invOut.save(output, "out");
        if (box != null) {
            output.putIntArray("box", new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invIn.load(input, "in");
        invOut.load(input, "out");
        box = input.getIntArray("box").filter(a -> a.length == 6)
            .map(a -> new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5])).orElse(null);
        // A scan in progress starts again
        resetScan();
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        Component name = getCustomName();
        return name != null ? name : getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerArchitectTable(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, invIn);
            Containers.dropContents(level, pos, invOut);
        }
    }
}
