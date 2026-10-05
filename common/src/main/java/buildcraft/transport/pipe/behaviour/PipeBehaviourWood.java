/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.transport.BCTransportConfig;

/** Wooden pipes pull items or fluids out of the block they point at when powered by an engine. */
public class PipeBehaviourWood extends PipeBehaviourDirectional implements IMjRedstoneReceiver {
    public PipeBehaviourWood(IPipe pipe) {
        super(pipe);
    }

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        return !(other instanceof PipeBehaviourWood);
    }

    @Override
    public boolean shouldForceConnection(Direction face, BlockEntity oTile) {
        // Connect to engines even though they hold no items
        return oTile.getLevel() != null
            && MjAPI.getConnector(oTile.getLevel(), oTile.getBlockPos(), face.getOpposite()) != null;
    }

    @Override
    protected boolean canFaceDirection(@Nullable Direction dir) {
        if (dir == null || pipe.getConnectedType(dir) != ConnectedType.TILE) {
            return false;
        }
        BlockEntity tile = pipe.getConnectedTile(dir);
        // Don't extract from engines
        return tile == null || tile.getLevel() == null
            || MjAPI.getConnector(tile.getLevel(), tile.getBlockPos(), dir.getOpposite()) == null;
    }

    @PipeEventHandler
    public void fluidSideCheck(PipeEventFluid.SideCheck sideCheck) {
        if (currentDir.face != null) {
            sideCheck.disallow(currentDir.face);
        }
    }

    protected long extract(long power, boolean simulate) {
        Direction dir = getCurrentDir();
        if (power <= 0 || dir == null) {
            return power;
        }
        if (pipe.getFlow() instanceof IFlowItems flow) {
            int maxItems = (int) (power / BCTransportConfig.mjPerItem);
            if (maxItems > 0) {
                int extracted = extractItems(flow, dir, maxItems, simulate);
                if (extracted > 0) {
                    return power - extracted * BCTransportConfig.mjPerItem;
                }
            }
        } else if (pipe.getFlow() instanceof IFlowFluid flow) {
            int maxMillibuckets = (int) (power / BCTransportConfig.mjPerMillibucket);
            if (maxMillibuckets > 0) {
                BCFluidStack extracted = extractFluid(flow, dir, maxMillibuckets, simulate);
                if (!extracted.isEmpty()) {
                    return power - extracted.getAmount() * BCTransportConfig.mjPerMillibucket;
                }
            }
        }
        return power;
    }

    protected int extractItems(IFlowItems flow, Direction dir, int count, boolean simulate) {
        return flow.tryExtractItems(count, dir, null, s -> true, simulate);
    }

    protected BCFluidStack extractFluid(IFlowFluid flow, Direction dir, int millibuckets, boolean simulate) {
        return flow.tryExtractFluid(millibuckets, dir, f -> true, simulate);
    }

    protected static Predicate<ItemStack> any() {
        return s -> true;
    }

    // MJ

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return this;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        final long power = 512 * MjAPI.MJ;
        return power - extract(power, true);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (pipe.getHolder().getPipeWorld().isClientSide()) return microJoules;
        return extract(microJoules, simulate);
    }
}
