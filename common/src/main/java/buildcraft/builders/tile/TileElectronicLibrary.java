/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersComponents;
import buildcraft.builders.container.ContainerElectronicLibrary;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.tile.TileBC;

/** Keeps a list of templates and blueprints. Written ones put in are added to the list (and handed back), and the
 * selected one can be copied onto blank (or written) ones. */
public class TileElectronicLibrary extends TileBC implements MenuProvider {
    public static final int MAX_ENTRIES = 1000;

    public final ItemHandlerSimple invDownIn = new ItemHandlerSimple(1, (slot, stack) -> stack.getItem() instanceof ItemSnapshot,
        this::setChanged);
    public final ItemHandlerSimple invDownOut = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);
    public final ItemHandlerSimple invUpIn = new ItemHandlerSimple(1,
        (slot, stack) -> stack.getItem() instanceof ItemSnapshot && ItemSnapshot.getHeader(stack) != null, this::setChanged);
    public final ItemHandlerSimple invUpOut = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);

    private final List<SnapshotHeader> entries = new ArrayList<>();
    private int selected = -1;

    public TileElectronicLibrary(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.LIBRARY_TILE.get(), pos, state);
    }

    public List<SnapshotHeader> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getSelected() {
        return selected;
    }

    public void select(int index) {
        selected = index >= 0 && index < entries.size() ? index : -1;
        setChanged();
    }

    public void remove(int index) {
        if (index < 0 || index >= entries.size()) return;
        entries.remove(index);
        if (selected >= entries.size()) selected = entries.size() - 1;
        setChanged();
        sendNetworkUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        // Upload: add the snapshot to the list
        ItemStack upIn = invUpIn.getItem(0);
        if (!upIn.isEmpty() && invUpOut.getItem(0).isEmpty()) {
            SnapshotHeader header = ItemSnapshot.getHeader(upIn);
            if (header != null && entries.size() < MAX_ENTRIES && entries.stream().noneMatch(e -> e.key().equals(header.key()))) {
                entries.add(header);
                sendNetworkUpdate();
            }
            invUpOut.setItem(0, upIn);
            invUpIn.setItem(0, ItemStack.EMPTY);
        }
        // Download: write the selected snapshot onto an item of the same kind
        ItemStack downIn = invDownIn.getItem(0);
        if (!downIn.isEmpty() && invDownOut.getItem(0).isEmpty() && selected >= 0 && selected < entries.size()) {
            SnapshotHeader header = entries.get(selected);
            if (downIn.getItem() instanceof ItemSnapshot item && item.type == header.type()) {
                ItemStack out = downIn.copyWithCount(1);
                out.set(BCBuildersComponents.SNAPSHOT.get(), header);
                downIn.shrink(1);
                invDownIn.setChanged();
                invDownOut.setItem(0, out);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invDownIn.save(output, "downIn");
        invDownOut.save(output, "downOut");
        invUpIn.save(output, "upIn");
        invUpOut.save(output, "upOut");
        output.store("entries", SnapshotHeader.CODEC.listOf(), entries);
        output.putInt("selected", selected);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invDownIn.load(input, "downIn");
        invDownOut.load(input, "downOut");
        invUpIn.load(input, "upIn");
        invUpOut.load(input, "upOut");
        entries.clear();
        entries.addAll(input.read("entries", SnapshotHeader.CODEC.listOf()).orElse(List.of()));
        selected = input.getIntOr("selected", -1);
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
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerElectronicLibrary(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, invDownIn);
            Containers.dropContents(level, pos, invDownOut);
            Containers.dropContents(level, pos, invUpIn);
            Containers.dropContents(level, pos, invUpOut);
        }
    }
}
