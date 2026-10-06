/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.world.item.Item;

import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.item;

public final class BCBuildersItems {
    public static RegistryEntry<Item, ItemSnapshot> TEMPLATE;
    public static RegistryEntry<Item, ItemSnapshot> BLUEPRINT;
    public static RegistryEntry<Item, ItemSchematicSingle> SCHEMATIC_SINGLE;
    public static RegistryEntry<Item, buildcraft.core.item.ItemVolumeBoxAddon> FILLER_PLANNER;

    private BCBuildersItems() {}

    static void init() {
        TEMPLATE = item("template", props -> new ItemSnapshot(props.stacksTo(16), Snapshot.Type.TEMPLATE));
        BLUEPRINT = item("blueprint", props -> new ItemSnapshot(props.stacksTo(16), Snapshot.Type.BLUEPRINT));
        SCHEMATIC_SINGLE = item("schematic_single", ItemSchematicSingle::new);
        FILLER_PLANNER = item("filler_planner",
            props -> new buildcraft.core.item.ItemVolumeBoxAddon(props, buildcraft.builders.addon.AddonFillerPlanner::new));
    }
}
