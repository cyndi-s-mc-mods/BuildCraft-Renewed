/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import org.jspecify.annotations.Nullable;

public abstract class PipeEvent {
    public final boolean canBeCancelled;
    public final IPipeHolder holder;
    private boolean canceled = false;

    public PipeEvent(IPipeHolder holder) {
        this.canBeCancelled = false;
        this.holder = holder;
    }

    protected PipeEvent(boolean canBeCancelled, IPipeHolder holder) {
        this.canBeCancelled = canBeCancelled;
        this.holder = holder;
    }

    public void cancel() {
        if (canBeCancelled) {
            canceled = true;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    /** @return A description of what's wrong with this event's state, or null if it's fine. */
    @Nullable
    public String checkStateForErrors() {
        if (canceled & !canBeCancelled) {
            return "Somehow cancelled an event that isn't marked as such!";
        }
        return null;
    }
}
