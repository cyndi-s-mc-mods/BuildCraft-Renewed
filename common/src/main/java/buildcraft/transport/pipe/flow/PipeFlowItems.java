/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.misc.DelayedList;
import buildcraft.lib.platform.Platform;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStone;

public final class PipeFlowItems extends PipeFlow implements IFlowItems {
    private static final double EXTRACT_SPEED = 0.08;

    private final DelayedList<TravellingItem> items = new DelayedList<>();
    /** Items loaded from disk or the network, waiting for the level time to be known. */
    private final List<TravellingItem> pendingLoad = new ArrayList<>();
    private final IItemTransactor[] transactors = new IItemTransactor[7];

    public PipeFlowItems(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        resolvePending();
        long tickNow = now();
        ValueOutput.ValueOutputList list = output.childrenList("items");
        for (List<TravellingItem> l : items.getAllElements()) {
            for (TravellingItem item : l) {
                item.save(list.addChild(), tickNow);
            }
        }
        for (TravellingItem item : pendingLoad) {
            item.save(list.addChild(), 0);
        }
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        items.clear();
        pendingLoad.clear();
        for (ValueInput child : input.childrenListOrEmpty("items")) {
            TravellingItem item = TravellingItem.load(child);
            if (!item.stack.isEmpty()) {
                pendingLoad.add(item);
            }
        }
    }

    private long now() {
        Level level = pipe.getHolder().getPipeWorld();
        return level == null ? 0 : level.getGameTime();
    }

    /** Adds items loaded from disk or the network once the level is available. */
    private void resolvePending() {
        if (pendingLoad.isEmpty() || pipe.getHolder().getPipeWorld() == null) return;
        long tickNow = now();
        for (TravellingItem item : pendingLoad) {
            item.resolveTimes(tickNow);
            items.add(item.getCurrentDelay(tickNow), item);
        }
        pendingLoad.clear();
    }

    private void sendItemDataToClient() {
        pipe.getHolder().scheduleNetworkUpdate();
    }

    @Override
    public void addDrops(List<ItemStack> toDrop) {
        super.addDrops(toDrop);
        resolvePending();
        for (List<TravellingItem> list : items.getAllElements()) {
            for (TravellingItem item : list) {
                if (!item.isPhantom) {
                    toDrop.add(item.stack);
                }
            }
        }
    }

    // IFlowItems

    @Override
    public int tryExtractItems(int count, Direction from, @Nullable DyeColor colour, Predicate<ItemStack> filter,
        boolean simulate) {
        IPipeHolder holder = pipe.getHolder();
        Level level = holder.getPipeWorld();
        if (level.isClientSide()) {
            throw new IllegalStateException("Cannot extract items on the client side!");
        }
        IItemTransactor trans = Platform.INSTANCE.getItemTransactor(level, holder.getPipePos().relative(from), from.getOpposite());
        if (trans == null) {
            return 0;
        }
        ItemStack possible = trans.extract(filter, 1, count, true);
        if (possible.isEmpty()) {
            return 0;
        }
        if (possible.getCount() > possible.getMaxStackSize()) {
            possible.setCount(possible.getMaxStackSize());
            count = possible.getMaxStackSize();
        }

        PipeEventItem.TryInsert tryInsert = new PipeEventItem.TryInsert(holder, this, colour, from, possible);
        holder.fireEvent(tryInsert);
        if (tryInsert.isCanceled() || tryInsert.accepted <= 0) {
            return 0;
        }

        count = Math.min(count, tryInsert.accepted);
        ItemStack stack = trans.extract(s -> ItemStack.isSameItemSameComponents(s, possible) && filter.test(s), count, count, simulate);
        if (stack.isEmpty()) {
            return 0;
        }
        if (!simulate) {
            insertItemEvents(stack, colour, EXTRACT_SPEED, from);
        }
        return stack.getCount();
    }

    @Override
    public @Nullable IItemTransactor getItemTransactor(@Nullable Direction side) {
        if (side == null) return null;
        int index = side.get3DDataValue();
        if (transactors[index] == null) {
            transactors[index] = new IItemTransactor() {
                @Override
                public ItemStack insert(ItemStack stack, boolean simulate) {
                    if (pipe.getHolder().getPipeWorld().isClientSide()) return stack;
                    return injectItem(stack, !simulate, side, null, 0.04);
                }

                @Override
                public ItemStack extract(Predicate<ItemStack> filter, int min, int max, boolean simulate) {
                    return ItemStack.EMPTY;
                }
            };
        }
        return transactors[index];
    }

    // PipeFlow

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof IFlowItems;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        Level level = oTile.getLevel();
        if (level == null) return false;
        return Platform.INSTANCE.getItemTransactor(level, oTile.getBlockPos(), face.getOpposite()) != null;
    }

    @Override
    public void onTick() {
        Level level = pipe.getHolder().getPipeWorld();
        resolvePending();
        List<TravellingItem> toTick = items.advance();
        long currentTime = level.getGameTime();

        for (TravellingItem item : toTick) {
            if (item.tickFinished > currentTime) {
                // Can happen if something ticks this block entity multiple times in a single real tick
                items.add((int) (item.tickFinished - currentTime), item);
                continue;
            }
            if (item.isPhantom || level.isClientSide()) {
                continue;
            }
            if (item.toCenter) {
                onItemReachCenter(item);
            } else {
                onItemReachEnd(item);
            }
        }
    }

    private void onItemReachCenter(TravellingItem item) {
        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.ReachCenter reachCenter = new PipeEventItem.ReachCenter(holder, this, item.colour, item.stack, item.side);
        holder.fireEvent(reachCenter);
        if (reachCenter.getStack().isEmpty()) {
            return;
        }

        PipeEventItem.SideCheck sideCheck = new PipeEventItem.SideCheck(holder, this, reachCenter.colour, reachCenter.from,
            reachCenter.getStack());
        sideCheck.disallow(reachCenter.from);
        for (Direction face : Direction.values()) {
            if (item.tried.contains(face) || !pipe.isConnected(face)) {
                sideCheck.disallow(face);
            }
        }
        holder.fireEvent(sideCheck);

        List<EnumSet<Direction>> order = sideCheck.getOrder();
        if (order.isEmpty()) {
            PipeEventItem.TryBounce tryBounce = new PipeEventItem.TryBounce(holder, this, reachCenter.colour,
                reachCenter.from, reachCenter.getStack());
            holder.fireEvent(tryBounce);
            if (tryBounce.canBounce) {
                order = List.of(EnumSet.of(reachCenter.from));
            } else {
                dropItem(item.stack, item.side.getOpposite(), item.speed);
                return;
            }
        }

        PipeEventItem.ItemEntry entry = new PipeEventItem.ItemEntry(reachCenter.colour, reachCenter.getStack(), reachCenter.from);
        PipeEventItem.Split split = new PipeEventItem.Split(holder, this, order, entry);
        holder.fireEvent(split);
        List<PipeEventItem.ItemEntry> entries = List.copyOf(split.items);

        PipeEventItem.FindDest findDest = new PipeEventItem.FindDest(holder, this, order, entries);
        holder.fireEvent(findDest);

        long now = holder.getPipeWorld().getGameTime();
        for (PipeEventItem.ItemEntry itemEntry : findDest.items) {
            if (itemEntry.stack.isEmpty()) {
                continue;
            }
            PipeEventItem.ModifySpeed modifySpeed = new PipeEventItem.ModifySpeed(holder, this, itemEntry, item.speed);
            final double newSpeed;
            if (holder.fireEvent(modifySpeed)) {
                double target = modifySpeed.targetSpeed;
                double maxDelta = modifySpeed.maxSpeedChange;
                if (item.speed < target) {
                    newSpeed = Math.min(target, item.speed + maxDelta);
                } else if (item.speed > target) {
                    newSpeed = Math.max(target, item.speed - maxDelta);
                } else {
                    newSpeed = item.speed;
                }
            } else {
                // Nothing affected the speed, so slow down a bit
                newSpeed = item.speed > 0.03 ? Math.max(0.03, item.speed - PipeBehaviourStone.SPEED_DELTA) : item.speed;
            }

            List<Direction> destinations = itemEntry.to;
            if (destinations == null || destinations.isEmpty()) {
                destinations = findDest.generateRandomOrder();
            }
            if (destinations.isEmpty()) {
                dropItem(itemEntry.stack, item.side.getOpposite(), newSpeed);
            } else {
                TravellingItem newItem = new TravellingItem(itemEntry.stack);
                newItem.tried.addAll(item.tried);
                newItem.toCenter = false;
                newItem.colour = itemEntry.colour;
                newItem.side = destinations.get(0);
                newItem.speed = newSpeed;
                newItem.genTimings(now, getPipeLength(newItem.side));
                items.add(newItem.timeToDest, newItem);
                sendItemDataToClient();
            }
        }
    }

    private void onItemReachEnd(TravellingItem item) {
        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.ReachEnd reachEnd = new PipeEventItem.ReachEnd(holder, this, item.colour, item.stack, item.side);
        holder.fireEvent(reachEnd);
        item.colour = reachEnd.colour;
        item.stack = reachEnd.getStack();
        ItemStack excess = item.stack;
        if (excess.isEmpty()) {
            return;
        }
        if (pipe.isConnected(item.side)) {
            ConnectedType type = pipe.getConnectedType(item.side);
            Direction oppositeSide = item.side.getOpposite();
            if (type == ConnectedType.PIPE) {
                IPipe oPipe = pipe.getConnectedPipe(item.side);
                if (oPipe != null && oPipe.getFlow() instanceof IFlowItems oFlow) {
                    ItemStack before = excess;
                    excess = oFlow.injectItem(excess.copy(), true, oppositeSide, item.colour, item.speed);
                    PipeEventItem.Ejected.IntoPipe event = new PipeEventItem.Ejected.IntoPipe(holder, this, before, excess,
                        item.side, oFlow);
                    holder.fireEvent(event);
                    excess = event.getExcess();
                }
            } else if (type == ConnectedType.TILE) {
                BlockPos target = holder.getPipePos().relative(item.side);
                IItemTransactor transactor = Platform.INSTANCE.getItemTransactor(holder.getPipeWorld(), target, oppositeSide);
                ItemStack before = excess;
                if (transactor != null) {
                    excess = transactor.insert(excess.copy(), false);
                }
                PipeEventItem.Ejected.IntoTile event = new PipeEventItem.Ejected.IntoTile(holder, this, before, excess,
                    item.side, pipe.getConnectedTile(item.side));
                holder.fireEvent(event);
                excess = event.getExcess();
            }
        }
        if (excess.isEmpty()) {
            sendItemDataToClient();
            return;
        }
        item.tried.add(item.side);
        item.toCenter = true;
        item.stack = excess;
        item.genTimings(holder.getPipeWorld().getGameTime(), getPipeLength(item.side));
        items.add(item.timeToDest, item);
        sendItemDataToClient();
    }

    private void dropItem(ItemStack stack, Direction motion, double speed) {
        if (stack.isEmpty()) {
            return;
        }
        IPipeHolder holder = pipe.getHolder();
        Level level = holder.getPipeWorld();
        BlockPos pos = holder.getPipePos();
        double x = pos.getX() + 0.5 + motion.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + motion.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + motion.getStepZ() * 0.5;
        speed += 0.01;
        speed *= 2;
        ItemEntity ent = new ItemEntity(level, x, y, z, stack);
        ent.setDeltaMovement(motion.getStepX() * speed, motion.getStepY() * speed, motion.getStepZ() * speed);
        PipeEventItem.Drop drop = new PipeEventItem.Drop(holder, this, ent);
        holder.fireEvent(drop);
        if (ent.getItem().isEmpty() || ent.isRemoved()) {
            return;
        }
        level.addFreshEntity(ent);
        sendItemDataToClient();
    }

    public boolean canInjectItems(Direction from) {
        return pipe.isConnected(from);
    }

    @Override
    public ItemStack injectItem(ItemStack stack, boolean doAdd, Direction from, @Nullable DyeColor colour, double speed) {
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (stack.isEmpty() || !canInjectItems(from)) {
            return stack;
        }
        speed = Math.max(0.01, speed);
        PipeEventItem.TryInsert tryInsert = new PipeEventItem.TryInsert(pipe.getHolder(), this, colour, from, stack);
        pipe.getHolder().fireEvent(tryInsert);
        if (tryInsert.isCanceled() || tryInsert.accepted <= 0) {
            return stack;
        }
        ItemStack toSplit = stack.copy();
        ItemStack toInsert = toSplit.split(tryInsert.accepted);
        if (doAdd) {
            insertItemEvents(toInsert, colour, speed, from);
        }
        return toSplit.isEmpty() ? ItemStack.EMPTY : toSplit;
    }

    @Override
    public void insertItemsForce(ItemStack stack, @Nullable Direction from, @Nullable DyeColor colour, double speed) {
        Level level = pipe.getHolder().getPipeWorld();
        if (level.isClientSide()) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (stack.isEmpty()) {
            return;
        }
        TravellingItem item = new TravellingItem(stack);
        if (from == null) {
            // Find a reasonable alternative (as it's not allowed to be null)
            Direction found = Direction.UP;
            for (Direction f : Direction.values()) {
                if (!pipe.isConnected(f)) {
                    found = f;
                    break;
                }
            }
            item.side = found;
        } else {
            item.side = from;
            item.tried.add(from);
        }
        item.toCenter = true;
        item.speed = Math.max(0.01, speed);
        item.colour = colour;
        item.genTimings(level.getGameTime(), 0);
        items.add(item.timeToDest, item);
    }

    private void insertItemEvents(ItemStack toInsert, @Nullable DyeColor colour, double speed, Direction from) {
        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.OnInsert onInsert = new PipeEventItem.OnInsert(holder, this, colour, toInsert, from);
        holder.fireEvent(onInsert);
        if (onInsert.getStack().isEmpty()) {
            return;
        }
        TravellingItem item = new TravellingItem(onInsert.getStack());
        item.side = from;
        item.toCenter = true;
        item.speed = speed;
        item.colour = onInsert.colour;
        item.genTimings(holder.getPipeWorld().getGameTime(), getPipeLength(from));
        item.tried.add(from);
        addItemTryMerge(item);
    }

    private void addItemTryMerge(TravellingItem item) {
        for (List<TravellingItem> list : items.getAllElements()) {
            for (TravellingItem item2 : list) {
                if (item2.mergeWith(item)) {
                    sendItemDataToClient();
                    return;
                }
            }
        }
        items.add(item.timeToDest, item);
        sendItemDataToClient();
    }

    public boolean doesContainItems() {
        return items.getMaxDelay() > 0 || !pendingLoad.isEmpty();
    }

    public double getPipeLength(Direction side) {
        if (pipe.isConnected(side)) {
            if (pipe.getConnectedType(side) == ConnectedType.TILE) {
                // Tiny distance for fully pushing items in.
                return 0.5 + 0.25;
            }
            return 0.5;
        }
        return 0.25;
    }

    /** @return Every item in the pipe, for rendering. */
    /** @return True if any items are moving through the pipe. */
    public boolean hasItems() {
        for (List<TravellingItem> list : items.getAllElements()) {
            if (!list.isEmpty()) return true;
        }
        return !pendingLoad.isEmpty();
    }

    public List<TravellingItem> getAllItemsForRender() {
        resolvePending();
        List<TravellingItem> all = new ArrayList<>();
        for (List<TravellingItem> innerList : items.getAllElements()) {
            all.addAll(innerList);
        }
        return all;
    }
}
