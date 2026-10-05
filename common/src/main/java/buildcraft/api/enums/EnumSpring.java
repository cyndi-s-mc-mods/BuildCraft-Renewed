/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.enums;

import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The kinds of spring: unbreakable blocks deep underground that keep making a fluid above them. */
public enum EnumSpring implements StringRepresentable {
    WATER(5, -1, () -> Blocks.WATER.defaultBlockState()),
    /** The oil is set by BuildCraft Energy. */
    OIL(6000, 8, null);

    /** How many ticks between each try. */
    public final int tickRate;
    /** One in how many tries makes fluid, or -1 for every try. */
    public final int chance;
    public @Nullable Supplier<BlockState> liquidBlock;

    private final String name = name().toLowerCase(Locale.ROOT);

    EnumSpring(int tickRate, int chance, @Nullable Supplier<BlockState> liquidBlock) {
        this.tickRate = tickRate;
        this.chance = chance;
        this.liquidBlock = liquidBlock;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
