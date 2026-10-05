/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeEventItem.ItemEntry;
import buildcraft.lib.misc.StackUtil;

public class PipeBehaviourDiamondItem extends PipeBehaviourDiamond {
    public PipeBehaviourDiamondItem(IPipe pipe) {
        super(pipe);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        ItemStack toCompare = sideCheck.stack;
        for (Direction face : Direction.values()) {
            if (sideCheck.isAllowed(face) && pipe.isConnected(face)) {
                int offset = FILTERS_PER_SIDE * face.get3DDataValue();
                boolean sideAllowed = false;
                boolean foundItem = false;
                for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                    ItemStack compareTo = filters.getItem(offset + i);
                    if (compareTo.isEmpty()) continue;
                    foundItem = true;
                    if (StackUtil.isMatchingItemOrList(compareTo, toCompare)) {
                        sideAllowed = true;
                        break;
                    }
                }
                if (foundItem) {
                    if (sideAllowed) {
                        sideCheck.increasePriority(face, 12);
                    } else {
                        sideCheck.disallow(face);
                    }
                }
            }
        }
    }

    /** Splits stacks between sides in proportion to how many of the item are in each side's filters. */
    @PipeEventHandler
    public void split(PipeEventItem.Split split) {
        Direction[] allSides = split.getAllPossibleDestinations().toArray(new Direction[0]);
        if (allSides.length <= 1) {
            return;
        }
        ItemEntry[] items = split.items.toArray(new ItemEntry[0]);
        split.items.clear();
        for (ItemEntry item : items) {
            int[] countPerSide = new int[allSides.length];
            int totalCount = 0;
            for (int s = 0; s < allSides.length; s++) {
                int offset = FILTERS_PER_SIDE * allSides[s].get3DDataValue();
                for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                    ItemStack compareTo = filters.getItem(offset + i);
                    if (StackUtil.isMatchingItemOrList(compareTo, item.stack)) {
                        int count = compareTo.getCount();
                        totalCount += count;
                        countPerSide[s] += count;
                    }
                }
            }
            if (totalCount == 0) {
                totalCount = allSides.length;
                Arrays.fill(countPerSide, 1);
            } else {
                int hcf = countPerSide[0];
                for (int c : countPerSide) {
                    hcf = StackUtil.findHighestCommonFactor(hcf, c);
                }
                if (hcf > 1) {
                    totalCount /= hcf;
                    for (int i = 0; i < countPerSide.length; i++) {
                        countPerSide[i] /= hcf;
                    }
                }
            }
            ItemEntry[] entries = new ItemEntry[allSides.length];
            ItemStack toSplit = item.stack;
            if (toSplit.getCount() >= totalCount) {
                int leftOver = toSplit.getCount() % totalCount;
                int multiples = (toSplit.getCount() - leftOver) / totalCount;
                for (int s = 0; s < allSides.length; s++) {
                    ItemStack toSide = toSplit.copyWithCount(countPerSide[s] * multiples);
                    entries[s] = new ItemEntry(item.colour, toSide, item.from);
                    entries[s].to = new ArrayList<>(List.of(allSides[s]));
                }
                toSplit.setCount(leftOver);
            }
            if (!toSplit.isEmpty()) {
                int[] randLookup = new int[totalCount];
                int j = 0;
                for (int s = 0; s < allSides.length; s++) {
                    int len = countPerSide[s];
                    Arrays.fill(randLookup, j, j + len, s);
                    j += len;
                }
                while (!toSplit.isEmpty()) {
                    int face = randLookup[split.holder.getPipeWorld().getRandom().nextInt(totalCount)];
                    if (entries[face] == null) {
                        ItemEntry entry = new ItemEntry(item.colour, toSplit.copyWithCount(1), item.from);
                        entry.to = new ArrayList<>(List.of(allSides[face]));
                        entries[face] = entry;
                    } else {
                        entries[face].stack.grow(1);
                    }
                    toSplit.shrink(1);
                }
            }
            for (ItemEntry entry : entries) {
                if (entry != null && !entry.stack.isEmpty()) {
                    split.items.add(entry);
                }
            }
        }
    }
}
