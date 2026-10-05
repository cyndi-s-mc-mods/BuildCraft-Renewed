/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeEventConnectionChange;
import buildcraft.api.transport.pipe.PipeFlow;

public final class Pipe implements IPipe {
    public final IPipeHolder holder;
    public final PipeDefinition definition;
    public final PipeBehaviour behaviour;
    public final PipeFlow flow;
    private @Nullable DyeColor colour = null;
    private boolean updateMarked = true;
    private final Map<Direction, ConnectedType> types = new EnumMap<>(Direction.class);

    public Pipe(IPipeHolder holder, PipeDefinition definition) {
        this.holder = holder;
        this.definition = definition;
        this.behaviour = definition.logic.apply(this);
        this.flow = definition.flowType.creator.apply(this);
    }

    public void save(ValueOutput output) {
        if (colour != null) {
            output.putInt("col", colour.getId());
        }
        behaviour.save(output.child("beh"));
        flow.save(output.child("flow"));
        int connectionData = 0;
        for (Direction face : Direction.values()) {
            ConnectedType type = types.get(face);
            if (type != null) {
                int data = type == ConnectedType.PIPE ? 0b01 : 0b10;
                connectionData |= data << (face.get3DDataValue() * 2);
            }
        }
        output.putInt("con", connectionData);
    }

    public void load(ValueInput input) {
        int col = input.getIntOr("col", -1);
        colour = col < 0 || !definition.canBeColoured ? null : DyeColor.byId(col);
        input.child("beh").ifPresent(behaviour::load);
        input.child("flow").ifPresent(flow::load);
        int connectionData = input.getIntOr("con", 0);
        types.clear();
        for (Direction face : Direction.values()) {
            int data = (connectionData >>> (face.get3DDataValue() * 2)) & 0b11;
            if (data == 0b01) {
                types.put(face, ConnectedType.PIPE);
            } else if (data == 0b10) {
                types.put(face, ConnectedType.TILE);
            }
        }
    }

    // IPipe

    @Override
    public IPipeHolder getHolder() {
        return holder;
    }

    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }

    @Override
    public PipeBehaviour getBehaviour() {
        return behaviour;
    }

    @Override
    public PipeFlow getFlow() {
        return flow;
    }

    @Override
    public @Nullable DyeColor getColour() {
        return colour;
    }

    @Override
    public void setColour(@Nullable DyeColor colour) {
        if (definition.canBeColoured && this.colour != colour) {
            this.colour = colour;
            markForUpdate();
            holder.scheduleBlockStateUpdate();
            holder.scheduleNetworkUpdate();
        }
    }

    @Override
    public void markForUpdate() {
        updateMarked = true;
    }

    public void onTick() {
        if (updateMarked) {
            // Ensure that the behaviour and flow always get valid connection data
            updateConnections();
        }
        behaviour.onTick();
        flow.onTick();
        if (updateMarked) {
            updateConnections();
        }
    }

    private void updateConnections() {
        if (holder.getPipeWorld().isClientSide()) {
            return;
        }
        updateMarked = false;
        Map<Direction, ConnectedType> old = new EnumMap<>(types);
        types.clear();

        for (Direction facing : Direction.values()) {
            BlockEntity oTile = holder.getNeighbourTile(facing);
            if (oTile == null) {
                continue;
            }
            IPipe oPipe = holder.getNeighbourPipe(facing);
            if (oPipe != null) {
                if (canPipesConnect(facing, this, oPipe)) {
                    types.put(facing, ConnectedType.PIPE);
                }
                continue;
            }
            if (behaviour.shouldForceConnection(facing, oTile) || flow.shouldForceConnection(facing, oTile)
                || (behaviour.canConnect(facing, oTile) && flow.canConnect(facing, oTile))) {
                types.put(facing, ConnectedType.TILE);
            }
        }

        if (!old.equals(types)) {
            for (Direction face : Direction.values()) {
                if (old.containsKey(face) != types.containsKey(face)) {
                    IPipe oPipe = holder.getNeighbourPipe(face);
                    if (oPipe != null) {
                        oPipe.markForUpdate();
                    }
                    holder.fireEvent(new PipeEventConnectionChange(holder, face));
                }
            }
            holder.scheduleBlockStateUpdate();
            holder.scheduleNetworkUpdate();
        }
    }

    public void addDrops(List<ItemStack> toDrop) {
        flow.addDrops(toDrop);
        behaviour.addDrops(toDrop);
    }

    public static boolean canPipesConnect(Direction to, IPipe one, IPipe two) {
        return canColoursConnect(one.getColour(), two.getColour())
            && one.getBehaviour().canConnect(to, two.getBehaviour())
            && two.getBehaviour().canConnect(to.getOpposite(), one.getBehaviour())
            && one.getFlow().canConnect(to, two.getFlow())
            && two.getFlow().canConnect(to.getOpposite(), one.getFlow());
    }

    public static boolean canColoursConnect(@Nullable DyeColor one, @Nullable DyeColor two) {
        return one == null || two == null || one == two;
    }

    @Override
    public @Nullable BlockEntity getConnectedTile(Direction side) {
        if (types.containsKey(side)) {
            BlockEntity offset = holder.getNeighbourTile(side);
            if (offset == null && !holder.getPipeWorld().isClientSide()) {
                markForUpdate();
            }
            return offset;
        }
        return null;
    }

    @Override
    public @Nullable IPipe getConnectedPipe(Direction side) {
        if (types.get(side) == ConnectedType.PIPE) {
            IPipe offset = holder.getNeighbourPipe(side);
            if (offset == null && !holder.getPipeWorld().isClientSide()) {
                markForUpdate();
            }
            return offset;
        }
        return null;
    }

    @Override
    public @Nullable ConnectedType getConnectedType(Direction side) {
        return types.get(side);
    }

    @Override
    public boolean isConnected(Direction side) {
        return types.containsKey(side);
    }
}
