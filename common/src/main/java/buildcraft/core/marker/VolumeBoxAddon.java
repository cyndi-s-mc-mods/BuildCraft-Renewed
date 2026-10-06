/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.marker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Something added to a corner of a volume box (such as a filler planner). Addons are added by right clicking a corner
 * with the addon's item, and removed by sneak right clicking the corner with the marker connector. */
public abstract class VolumeBoxAddon {
    private static final Map<Identifier, Supplier<? extends VolumeBoxAddon>> TYPES = new LinkedHashMap<>();

    public static void register(Identifier type, Supplier<? extends VolumeBoxAddon> factory) {
        TYPES.put(type, factory);
    }

    @Nullable
    public static VolumeBoxAddon create(Identifier type) {
        Supplier<? extends VolumeBoxAddon> factory = TYPES.get(type);
        return factory == null ? null : factory.get();
    }

    public abstract Identifier getType();

    /** @return The item that adds this addon, given back when it's removed. */
    public abstract Item getItem();

    /** @return The block atlas sprite drawn on the addon's corner. */
    public abstract Identifier getSprite();

    /** Called when the box changes size (or the addon has just been added or loaded). */
    public void onBoxChanged(VolumeBoxEntity box) {}

    public void onRightClick(VolumeBoxEntity box, ServerPlayer player) {}

    /** @return The positions (as indexes into the box, see {@link VolumeBoxEntity#indexOf}) to draw a preview at, or
     *         null for none. */
    public boolean @Nullable [] getPreview(VolumeBoxEntity box) {
        return null;
    }

    public abstract void save(ValueOutput output);

    public abstract void load(ValueInput input);
}
