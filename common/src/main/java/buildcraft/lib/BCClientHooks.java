/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

/** Client-only actions that common code (such as items) can ask for. The client sets these when it starts, so that
 * dedicated servers never load client classes. */
public final class BCClientHooks {
    /** Opens the guide book. */
    public static Runnable openGuide = () -> {};

    private BCClientHooks() {}
}
