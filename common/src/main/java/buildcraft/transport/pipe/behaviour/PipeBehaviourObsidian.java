/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.List;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Obsidian pipes pick up items that touch their open end. When powered by an engine they pull items from further
 * away. */
public class PipeBehaviourObsidian extends PipeBehaviour implements IMjRedstoneReceiver {
    private static final long POWER_PER_ITEM = MjAPI.MJ / 2;
    private static final long POWER_PER_METRE = MjAPI.MJ / 4;
    private static final double INSERT_SPEED = 0.04;
    private static final int DROP_GAP = 20;

    private final WeakHashMap<ItemEntity, Long> entityDropTime = new WeakHashMap<>();
    private int toWaitTicks = DROP_GAP;

    public PipeBehaviourObsidian(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void onTick() {
        if (!pipe.getHolder().getPipeWorld().isClientSide() && toWaitTicks > 0) {
            toWaitTicks--;
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        return !(other instanceof PipeBehaviourObsidian);
    }

    @Override
    public void onEntityCollide(Entity entity) {
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            return;
        }
        Direction openFace = getOpenFace();
        if (openFace != null) {
            trySuckEntity(entity, openFace, Long.MAX_VALUE, false);
        }
    }

    private @Nullable Direction getOpenFace() {
        Direction openFace = null;
        for (Direction face : Direction.values()) {
            if (pipe.isConnected(face)) {
                if (openFace == null) {
                    openFace = face.getOpposite();
                } else {
                    return null;
                }
            }
        }
        return openFace;
    }

    private AABB getSuckingBox(Direction openFace, int distance) {
        BlockPos pos = pipe.getHolder().getPipePos();
        AABB bb = new AABB(pos).deflate(0.1);
        return switch (openFace) {
            case WEST -> bb.move(-distance, 0, 0).inflate(0.5, distance, distance);
            case EAST -> bb.move(distance, 0, 0).inflate(0.5, distance, distance);
            case DOWN -> bb.move(0, -distance, 0).inflate(distance, 0.5, distance);
            case UP -> bb.move(0, distance, 0).inflate(distance, 0.5, distance);
            case NORTH -> bb.move(0, 0, -distance).inflate(distance, distance, 0.5);
            case SOUTH -> bb.move(0, 0, distance).inflate(distance, distance, 0.5);
        };
    }

    private long trySuckEntity(Entity entity, Direction faceFrom, long power, boolean simulate) {
        if (entity.isRemoved() || entity instanceof LivingEntity || !(entity instanceof ItemEntity itemEntity)) {
            return power;
        }
        Long tickPickup = entityDropTime.get(itemEntity);
        if (tickPickup != null) {
            if (pipe.getHolder().getPipeWorld().getGameTime() < tickPickup) {
                return power;
            }
            entityDropTime.remove(itemEntity);
        }
        if (!(pipe.getFlow() instanceof IFlowItems flowItem)) {
            return power;
        }
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return power;
        long powerReqPerItem;
        int max;
        if (power == Long.MAX_VALUE) {
            max = stack.getCount();
            powerReqPerItem = 0;
        } else {
            double distance = Math.sqrt(entity.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pipe.getHolder().getPipePos())));
            powerReqPerItem = (long) (Math.max(1, distance) * POWER_PER_METRE + POWER_PER_ITEM);
            max = (int) Math.min(stack.getCount(), power / powerReqPerItem);
        }
        if (max <= 0) return power;
        if (!simulate) {
            ItemStack taken = stack.split(max);
            itemEntity.setItem(stack);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            flowItem.insertItemsForce(taken, faceFrom, null, INSERT_SPEED);
        }
        return power - powerReqPerItem * max;
    }

    @PipeEventHandler
    public void onPipeDrop(PipeEventItem.Drop drop) {
        entityDropTime.put(drop.getEntity(), pipe.getHolder().getPipeWorld().getGameTime() + DROP_GAP);
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
        return power - receivePower(power, true);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        Level level = pipe.getHolder().getPipeWorld();
        if (toWaitTicks > 0 || level.isClientSide()) {
            return microJoules;
        }
        Direction openFace = getOpenFace();
        if (openFace == null) {
            return microJoules;
        }
        for (int d = 1; d < 5; d++) {
            List<ItemEntity> discovered = level.getEntitiesOfClass(ItemEntity.class, getSuckingBox(openFace, d));
            for (ItemEntity entity : discovered) {
                long leftOver = trySuckEntity(entity, openFace, microJoules, simulate);
                if (leftOver < microJoules) {
                    return leftOver;
                }
            }
        }
        return microJoules - MjAPI.MJ;
    }
}
