/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A simple store of MJ power. */
public class MjBattery {
    private final long capacity;
    private long microJoules = 0;

    public MjBattery(long capacity) {
        this.capacity = capacity;
    }

    public void save(ValueOutput output, String key) {
        output.putLong(key, microJoules);
    }

    public void load(ValueInput input, String key) {
        microJoules = input.getLongOr(key, 0);
    }

    /** @return The power that was not added (always 0: batteries can overfill, but then leak). */
    public long addPower(long microJoulesToAdd, boolean simulate) {
        if (!simulate) {
            this.microJoules += microJoulesToAdd;
        }
        return 0;
    }

    /** Like {@link #addPower}, but refuses all power when already full. */
    public long addPowerChecking(long microJoulesToAdd, boolean simulate) {
        if (isFull()) {
            return microJoulesToAdd;
        }
        return addPower(microJoulesToAdd, simulate);
    }

    public long extractAll() {
        return extractPower(0, microJoules);
    }

    public boolean extractPower(long power) {
        return extractPower(power, power) > 0;
    }

    public long extractPower(long min, long max) {
        if (microJoules < min) return 0;
        long extracting = Math.min(microJoules, max);
        microJoules -= extracting;
        return extracting;
    }

    public boolean isFull() {
        return microJoules >= capacity;
    }

    public long getStored() {
        return microJoules;
    }

    public void setStored(long microJoules) {
        this.microJoules = microJoules;
    }

    public long getCapacity() {
        return capacity;
    }

    /** Leaks power when holding more than twice the capacity. */
    public void tick() {
        if (microJoules > capacity * 2) {
            long diff = microJoules - capacity * 2;
            microJoules -= (diff / 32) + (diff % 32 == 0 ? 0 : 1);
        }
    }

    public String getDebugString() {
        return MjAPI.formatMj(microJoules) + " / " + MjAPI.formatMj(capacity) + " MJ";
    }
}
