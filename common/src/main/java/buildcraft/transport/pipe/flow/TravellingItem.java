/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** An item moving through a pipe.
 * <ul>
 * <li>While {@link #toCenter} is true it is moving from {@link #side} to the centre of the pipe.</li>
 * <li>Otherwise it is moving from the centre towards {@link #side}.</li>
 * </ul>
 */
public class TravellingItem {
    public @Nullable DyeColor colour;
    ItemStack stack;
    boolean toCenter;
    double speed = 0.05;
    long tickStarted, tickFinished;
    int timeToDest;
    Direction side = Direction.UP;
    EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);
    boolean isPhantom = false;

    public TravellingItem(ItemStack stack) {
        this.stack = stack;
    }

    /** Loads an item whose times are relative to now (they are made absolute by {@link #resolveTimes}). */
    static TravellingItem load(ValueInput input) {
        TravellingItem item = new TravellingItem(input.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        int c = input.getIntOr("colour", -1);
        item.colour = c < 0 ? null : DyeColor.byId(c);
        item.toCenter = input.getBooleanOr("toCenter", true);
        item.speed = Math.max(0.001, input.getDoubleOr("speed", 0.05));
        item.tickStarted = input.getIntOr("tickStarted", 0);
        item.tickFinished = input.getIntOr("tickFinished", 0);
        item.timeToDest = input.getIntOr("timeToDest", 0);
        item.side = Direction.from3DDataValue(input.getIntOr("side", 1));
        if (item.timeToDest == 0) {
            item.toCenter = true;
        }
        int tried = input.getIntOr("tried", 0);
        for (Direction dir : Direction.values()) {
            if ((tried & (1 << dir.get3DDataValue())) != 0) {
                item.tried.add(dir);
            }
        }
        item.isPhantom = input.getBooleanOr("isPhantom", false);
        return item;
    }

    void resolveTimes(long tickNow) {
        tickStarted += tickNow;
        tickFinished += tickNow;
    }

    void save(ValueOutput output, long tickNow) {
        output.store("stack", ItemStack.OPTIONAL_CODEC, stack);
        if (colour != null) {
            output.putInt("colour", colour.getId());
        }
        output.putBoolean("toCenter", toCenter);
        output.putDouble("speed", speed);
        output.putInt("tickStarted", (int) (tickStarted - tickNow));
        output.putInt("tickFinished", (int) (tickFinished - tickNow));
        output.putInt("timeToDest", timeToDest);
        output.putInt("side", side.get3DDataValue());
        int triedBits = 0;
        for (Direction dir : tried) {
            triedBits |= 1 << dir.get3DDataValue();
        }
        output.putInt("tried", triedBits);
        if (isPhantom) {
            output.putBoolean("isPhantom", true);
        }
    }

    public ItemStack getStack() {
        return stack;
    }

    public int getCurrentDelay(long tickNow) {
        long diff = tickFinished - tickNow;
        return diff < 0 ? 0 : (int) diff;
    }

    public void genTimings(long now, double distance) {
        tickStarted = now;
        timeToDest = (int) Math.ceil(distance / speed);
        tickFinished = now + timeToDest;
    }

    public boolean canMerge(TravellingItem with) {
        if (isPhantom || with.isPhantom) {
            return false;
        }
        return toCenter == with.toCenter
            && colour == with.colour
            && side == with.side
            && Math.abs(tickFinished - with.tickFinished) < 4
            && stack.getMaxStackSize() >= stack.getCount() + with.stack.getCount()
            && ItemStack.isSameItemSameComponents(stack, with.stack);
    }

    public boolean mergeWith(TravellingItem with) {
        if (canMerge(with)) {
            this.stack.grow(with.stack.getCount());
            return true;
        }
        return false;
    }

    /** @return The position of the item relative to the pipe block's corner. */
    public Vec3 getRenderPosition(long tick, float partialTicks, PipeFlowItems flow) {
        long diff = tickFinished - tickStarted;
        long afterTick = tick - tickStarted;
        float interp = diff <= 0 ? 1 : (afterTick + partialTicks) / diff;
        interp = Math.max(0, Math.min(1, interp));
        Vec3 center = new Vec3(0.5, 0.5, 0.5);
        double length = flow.getPipeLength(side);
        Vec3 vecSide = center.add(side.getStepX() * length, side.getStepY() * length, side.getStepZ() * length);
        Vec3 vecFrom = toCenter ? vecSide : center;
        Vec3 vecTo = toCenter ? center : vecSide;
        return vecFrom.scale(1 - interp).add(vecTo.scale(interp));
    }
}
