/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.List;

/** A list of elements that each become available after a delay (in calls to {@link #advance()}). */
public class DelayedList<E> {
    private final List<List<E>> elements = new ArrayList<>();

    public int getMaxDelay() {
        return elements.size();
    }

    /** @return The elements whose delay has run out. */
    public List<E> advance() {
        if (elements.isEmpty()) {
            return List.of();
        }
        return elements.remove(0);
    }

    public void add(int delay, E element) {
        if (delay < 0) {
            delay = 0;
        }
        while (elements.size() < delay + 1) {
            elements.add(new ArrayList<>());
        }
        elements.get(delay).add(element);
    }

    public List<List<E>> getAllElements() {
        return elements;
    }

    public void clear() {
        elements.clear();
    }
}
