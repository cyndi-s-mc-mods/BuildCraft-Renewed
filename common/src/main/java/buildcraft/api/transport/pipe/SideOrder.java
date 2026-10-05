/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.Direction;

/** Which sides something may go out of, and in what order of preference. Shared by the item and fluid side checks. */
public class SideOrder {
    private final int[] priority = new int[6];
    private final EnumSet<Direction> allowed = EnumSet.allOf(Direction.class);

    public boolean isAllowed(Direction side) {
        return allowed.contains(side);
    }

    public void disallow(Direction... sides) {
        for (Direction side : sides) {
            allowed.remove(side);
        }
    }

    public void disallowAll(Collection<Direction> sides) {
        allowed.removeAll(sides);
    }

    public void disallowAllExcept(Direction... sides) {
        allowed.retainAll(Arrays.asList(sides));
    }

    public void disallowAllExcept(Collection<Direction> sides) {
        allowed.retainAll(sides);
    }

    public void disallowAll() {
        allowed.clear();
    }

    public void increasePriority(Direction side) {
        increasePriority(side, 1);
    }

    public void increasePriority(Direction side, int by) {
        priority[side.get3DDataValue()] -= by;
    }

    public void decreasePriority(Direction side) {
        decreasePriority(side, 1);
    }

    public void decreasePriority(Direction side, int by) {
        increasePriority(side, -by);
    }

    /** @return The allowed sides, grouped by priority (highest priority first). */
    public List<EnumSet<Direction>> getOrder() {
        List<EnumSet<Direction>> list = new ArrayList<>();
        if (allowed.isEmpty()) {
            return list;
        }
        int[] ordered = Arrays.copyOf(priority, 6);
        Arrays.sort(ordered);
        for (int i = 0; i < 6; i++) {
            int current = ordered[i];
            if (i != 0 && current == ordered[i - 1]) {
                continue;
            }
            EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
            for (Direction face : Direction.values()) {
                if (allowed.contains(face) && priority[face.get3DDataValue()] == current) {
                    set.add(face);
                }
            }
            if (!set.isEmpty()) {
                list.add(set);
            }
        }
        return list;
    }

    /** @return The allowed sides with the highest priority. */
    public EnumSet<Direction> getHighestPriority() {
        List<EnumSet<Direction>> order = getOrder();
        return order.isEmpty() ? EnumSet.noneOf(Direction.class) : order.get(0);
    }
}
