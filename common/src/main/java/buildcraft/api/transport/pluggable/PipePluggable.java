/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pluggable;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.transport.pipe.IPipeHolder;

/** Something attached to one side of a pipe, like a gate, plug or facade. */
public abstract class PipePluggable {
    public final PluggableDefinition definition;
    public final IPipeHolder holder;
    public final Direction side;

    public PipePluggable(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        this.definition = definition;
        this.holder = holder;
        this.side = side;
    }

    /** Saves this pluggable. Also used to send it to clients, so anything the renderer needs must be saved. */
    public void save(ValueOutput output) {}

    public final void scheduleNetworkUpdate() {
        holder.scheduleNetworkUpdate();
    }

    public void onTick() {}

    /** @return The box (in block coordinates, 0-1) used for collisions and clicking. */
    public abstract AABB getBoundingBox();

    /** @return True if this stops the pipe from connecting on its side. */
    public boolean isBlocking() {
        return false;
    }

    /** Called when this pluggable is removed from the pipe. */
    public void onRemove() {}

    public void addDrops(List<ItemStack> toDrop) {
        ItemStack stack = getPickStack();
        if (!stack.isEmpty()) {
            toDrop.add(stack);
        }
    }

    /** @return The item for this pluggable. */
    public ItemStack getPickStack() {
        return ItemStack.EMPTY;
    }

    public InteractionResult onPluggableActivate(Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public void onPlacedBy(@Nullable Player player) {}

    /** @return An MJ connector to expose on this pluggable's side, or null. */
    @Nullable
    public IMjConnector getMjConnector() {
        return null;
    }

    public boolean canConnectToRedstone() {
        return false;
    }

    /** @return The redstone signal (0-15) this pluggable makes the pipe give out of the given side. */
    public int getRedstoneOutput(Direction pipeSide) {
        return 0;
    }

    /** @return True if this pluggable (such as a gate) is sending a signal along wires of the given colour. */
    public boolean isEmittingWire(net.minecraft.world.item.DyeColor colour) {
        return false;
    }

    /** @return The model to draw, as it would be on the west side. Only called on the client. */
    public List<PlugModelPart> getModel() {
        return List.of();
    }
}
