/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class PipeEventItem extends PipeEvent {
    public final IFlowItems flow;

    protected PipeEventItem(IPipeHolder holder, IFlowItems flow) {
        super(holder);
        this.flow = flow;
    }

    protected PipeEventItem(boolean canBeCancelled, IPipeHolder holder, IFlowItems flow) {
        super(canBeCancelled, holder);
        this.flow = flow;
    }

    /** Fired when something tries to insert an item into the pipe. Reduce {@link #accepted} to accept fewer. */
    public static class TryInsert extends PipeEventItem {
        public final @Nullable DyeColor colour;
        public final Direction from;
        public final ItemStack attempting;
        public int accepted;

        public TryInsert(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, Direction from, ItemStack attempting) {
            super(true, holder, flow);
            this.colour = colour;
            this.from = from;
            this.attempting = attempting;
            this.accepted = attempting.getCount();
        }
    }

    public abstract static class ReachDest extends PipeEventItem {
        public @Nullable DyeColor colour;
        private ItemStack stack;

        public ReachDest(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, ItemStack stack) {
            super(holder, flow);
            this.colour = colour;
            this.stack = stack;
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setStack(ItemStack stack) {
            this.stack = stack;
        }
    }

    public static class OnInsert extends ReachDest {
        public final Direction from;

        public OnInsert(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, ItemStack stack, Direction from) {
            super(holder, flow, colour, stack);
            this.from = from;
        }
    }

    public static class ReachCenter extends ReachDest {
        public final Direction from;

        public ReachCenter(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, ItemStack stack, Direction from) {
            super(holder, flow, colour, stack);
            this.from = from;
        }
    }

    public static class ReachEnd extends ReachDest {
        public final Direction to;

        public ReachEnd(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, ItemStack stack, Direction to) {
            super(holder, flow, colour, stack);
            this.to = to;
        }
    }

    public abstract static class Ejected extends PipeEventItem {
        public final ItemStack inserted;
        private ItemStack excess;
        public final Direction to;

        protected Ejected(IPipeHolder holder, IFlowItems flow, ItemStack inserted, ItemStack excess, Direction to) {
            super(holder, flow);
            this.inserted = inserted;
            this.excess = excess;
            this.to = to;
        }

        public ItemStack getExcess() {
            return excess;
        }

        public void setExcess(ItemStack stack) {
            this.excess = stack;
        }

        public static class IntoPipe extends Ejected {
            public final IFlowItems otherPipe;

            public IntoPipe(IPipeHolder holder, IFlowItems flow, ItemStack inserted, ItemStack excess, Direction to,
                IFlowItems otherPipe) {
                super(holder, flow, inserted, excess, to);
                this.otherPipe = otherPipe;
            }
        }

        public static class IntoTile extends Ejected {
            public final @Nullable BlockEntity tile;

            public IntoTile(IPipeHolder holder, IFlowItems flow, ItemStack inserted, ItemStack excess, Direction to,
                @Nullable BlockEntity tile) {
                super(holder, flow, inserted, excess, to);
                this.tile = tile;
            }
        }
    }

    /** Fired to decide which sides an item at the centre of the pipe may go to. */
    public static class SideCheck extends PipeEventItem {
        public final @Nullable DyeColor colour;
        public final Direction from;
        public final ItemStack stack;
        public final SideOrder order = new SideOrder();

        public SideCheck(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, Direction from, ItemStack stack) {
            super(holder, flow);
            this.colour = colour;
            this.from = from;
            this.stack = stack;
        }

        public boolean isAllowed(Direction side) {
            return order.isAllowed(side);
        }

        public void disallow(Direction... sides) {
            order.disallow(sides);
        }

        public void disallowAllExcept(Direction... sides) {
            order.disallowAllExcept(sides);
        }

        public void disallowAll() {
            order.disallowAll();
        }

        public void increasePriority(Direction side, int by) {
            order.increasePriority(side, by);
        }

        public void decreasePriority(Direction side, int by) {
            order.decreasePriority(side, by);
        }

        public List<EnumSet<Direction>> getOrder() {
            return order.getOrder();
        }
    }

    /** Fired when an item has nowhere to go. Set {@link #canBounce} to send it back the way it came. */
    public static class TryBounce extends PipeEventItem {
        public final @Nullable DyeColor colour;
        public final Direction from;
        public final ItemStack stack;
        public boolean canBounce = false;

        public TryBounce(IPipeHolder holder, IFlowItems flow, @Nullable DyeColor colour, Direction from, ItemStack stack) {
            super(holder, flow);
            this.colour = colour;
            this.from = from;
            this.stack = stack;
        }
    }

    /** Fired when an item is dropped out of the pipe as an entity. */
    public static class Drop extends PipeEventItem {
        private final ItemEntity entity;

        public Drop(IPipeHolder holder, IFlowItems flow, ItemEntity entity) {
            super(holder, flow);
            this.entity = entity;
        }

        public ItemStack getStack() {
            return entity.getItem();
        }

        public void setStack(ItemStack stack) {
            entity.setItem(stack);
        }

        public ItemEntity getEntity() {
            return entity;
        }
    }

    public abstract static class OrderedEvent extends PipeEventItem {
        public final List<EnumSet<Direction>> orderedDestinations;

        public OrderedEvent(IPipeHolder holder, IFlowItems flow, List<EnumSet<Direction>> orderedDestinations) {
            super(holder, flow);
            this.orderedDestinations = orderedDestinations;
        }

        public EnumSet<Direction> getAllPossibleDestinations() {
            EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
            for (EnumSet<Direction> e : orderedDestinations) {
                set.addAll(e);
            }
            return set;
        }

        public List<Direction> generateRandomOrder() {
            List<Direction> list = new ArrayList<>();
            for (EnumSet<Direction> set : orderedDestinations) {
                List<Direction> faces = new ArrayList<>(set);
                Collections.shuffle(faces);
                list.addAll(faces);
            }
            return list;
        }
    }

    /** Fired to split an item stack between several destinations. */
    public static class Split extends OrderedEvent {
        public final List<ItemEntry> items = new ArrayList<>();

        public Split(IPipeHolder holder, IFlowItems flow, List<EnumSet<Direction>> order, ItemEntry toSplit) {
            super(holder, flow, order);
            items.add(toSplit);
        }
    }

    /** Fired to pick a destination for each item. */
    public static class FindDest extends OrderedEvent {
        public final List<ItemEntry> items;

        public FindDest(IPipeHolder holder, IFlowItems flow, List<EnumSet<Direction>> orderedDestinations, List<ItemEntry> items) {
            super(holder, flow, orderedDestinations);
            this.items = items;
        }
    }

    /** Fired to change how fast an item travels. */
    public static class ModifySpeed extends PipeEventItem {
        public final ItemEntry item;
        public final double currentSpeed;
        public double targetSpeed = 0;
        public double maxSpeedChange = 0;

        public ModifySpeed(IPipeHolder holder, IFlowItems flow, ItemEntry item, double initSpeed) {
            super(holder, flow);
            this.item = item;
            currentSpeed = initSpeed;
        }

        public void modifyTo(double target, double maxDelta) {
            targetSpeed = target;
            maxSpeedChange = maxDelta;
        }
    }

    public static class ItemEntry {
        public final @Nullable DyeColor colour;
        public final ItemStack stack;
        public final Direction from;
        /** The sides the item will try to go to, in order. Null to pick one at random. */
        public @Nullable List<Direction> to;

        public ItemEntry(@Nullable DyeColor colour, ItemStack stack, Direction from) {
            this.colour = colour;
            this.stack = stack;
            this.from = from;
        }
    }
}
