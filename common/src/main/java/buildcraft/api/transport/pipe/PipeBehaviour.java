/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjConnector;

/** What kind of pipe this is (wood, iron, diamond...). Handles pipe events with methods annotated with
 * {@link PipeEventHandler}. */
public abstract class PipeBehaviour {
    public final IPipe pipe;

    public PipeBehaviour(IPipe pipe) {
        this.pipe = pipe;
    }

    public void save(ValueOutput output) {}

    public void load(ValueInput input) {}

    /** Sets the behaviour-specific block state properties, such as which side a wooden pipe extracts from. */
    public BlockState updateBlockState(BlockState state) {
        return state;
    }

    public boolean canConnect(Direction face, PipeBehaviour other) {
        return true;
    }

    public boolean canConnect(Direction face, BlockEntity oTile) {
        return true;
    }

    public boolean shouldForceConnection(Direction face, BlockEntity oTile) {
        return false;
    }

    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        return InteractionResult.PASS;
    }

    /** @return The GUI to open when the pipe is right clicked, or null for none. */
    @Nullable
    public MenuProvider getMenuProvider() {
        return null;
    }

    public void onEntityCollide(Entity entity) {}

    public void onTick() {}

    /** @return The MJ connector on a side, if this behaviour handles power (like wooden pipes extracting). */
    @Nullable
    public IMjConnector getMjConnector(Direction side) {
        return null;
    }

    public void addDrops(List<ItemStack> toDrop) {}
}
