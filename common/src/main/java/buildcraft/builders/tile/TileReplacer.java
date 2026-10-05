/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import buildcraft.builders.container.ContainerReplacer;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotHeader;
import buildcraft.builders.snapshot.SnapshotStore;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.tile.TileBC;

/** Changes one block for another throughout a blueprint: put in the blueprint, and single schematics of the block to
 * replace and the block to replace it with. The schematics are used up. */
public class TileReplacer extends TileBC implements MenuProvider {
    public final ItemHandlerSimple invSnapshot = new ItemHandlerSimple(1, (slot, stack) -> {
        SnapshotHeader header = ItemSnapshot.getHeader(stack);
        return header != null && header.type() == Snapshot.Type.BLUEPRINT;
    }, this::setChanged);
    public final ItemHandlerSimple invFrom = new ItemHandlerSimple(1,
        (slot, stack) -> ItemSchematicSingle.getState(stack) != null, this::setChanged);
    public final ItemHandlerSimple invTo = new ItemHandlerSimple(1,
        (slot, stack) -> ItemSchematicSingle.getState(stack) != null, this::setChanged);

    public TileReplacer(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.REPLACER_TILE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        ItemStack blueprint = invSnapshot.getItem(0);
        BlockState from = ItemSchematicSingle.getState(invFrom.getItem(0));
        BlockState to = ItemSchematicSingle.getState(invTo.getItem(0));
        SnapshotHeader header = ItemSnapshot.getHeader(blueprint);
        if (header == null || from == null || to == null) return;
        Snapshot snapshot = SnapshotStore.get(level.getServer(), header.key());
        if (snapshot == null || snapshot.type != Snapshot.Type.BLUEPRINT) return;
        List<BlockState> palette = new ArrayList<>();
        for (BlockState state : snapshot.palette) {
            // Keeps what properties it can, such as which way stairs face
            palette.add(state.is(from.getBlock()) ? to.getBlock().withPropertiesOf(state) : state);
        }
        Snapshot replaced = Snapshot.blueprint(snapshot.sizeX, snapshot.sizeY, snapshot.sizeZ, snapshot.facing, snapshot.offset,
            palette, snapshot.data.clone());
        UUID key = SnapshotStore.add(level.getServer(), replaced);
        blueprint.set(BCBuildersComponents.SNAPSHOT.get(), SnapshotHeader.of(key, replaced, header.name()));
        invSnapshot.setChanged();
        invFrom.setItem(0, ItemStack.EMPTY);
        invTo.setItem(0, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invSnapshot.save(output, "snapshot");
        invFrom.save(output, "from");
        invTo.save(output, "to");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invSnapshot.load(input, "snapshot");
        invFrom.load(input, "from");
        invTo.load(input, "to");
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
        return new ContainerReplacer(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, invSnapshot);
            Containers.dropContents(level, pos, invFrom);
            Containers.dropContents(level, pos, invTo);
        }
    }
}
