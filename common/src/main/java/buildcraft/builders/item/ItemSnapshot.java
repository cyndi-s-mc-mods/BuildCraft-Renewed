/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.builders.BCBuildersComponents;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotHeader;

/** A blank or written template or blueprint. Written ones hold a {@link SnapshotHeader}. */
public class ItemSnapshot extends Item {
    public final Snapshot.Type type;

    public ItemSnapshot(Properties properties, Snapshot.Type type) {
        super(properties);
        this.type = type;
    }

    @Nullable
    public static SnapshotHeader getHeader(ItemStack stack) {
        return stack.get(BCBuildersComponents.SNAPSHOT.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        SnapshotHeader header = getHeader(stack);
        Component base = super.getName(stack);
        if (header == null) {
            return Component.translatable("item.buildcraft.snapshot.clean", base);
        }
        if (!header.name().isEmpty()) {
            return Component.translatable("item.buildcraft.snapshot.named", base, header.name());
        }
        return Component.translatable("item.buildcraft.snapshot.used", base, header.sizeText());
    }
}
