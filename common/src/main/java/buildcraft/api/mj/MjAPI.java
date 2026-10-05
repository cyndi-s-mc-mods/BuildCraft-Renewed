/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.mj;

import java.text.DecimalFormat;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MjAPI {
    /** A single minecraft joule, in micro joules (the power system base unit). */
    public static final long ONE_MINECRAFT_JOULE = 1_000_000L;
    /** The same as {@link #ONE_MINECRAFT_JOULE}, but a shorter field name. */
    public static final long MJ = ONE_MINECRAFT_JOULE;

    /** The decimal format used to display values of MJ to the player. */
    public static final DecimalFormat MJ_DISPLAY_FORMAT = new DecimalFormat("#,##0.##");

    private MjAPI() {}

    /** Formats a given MJ value to a player-oriented string. Note that this does not append "MJ" to the value. */
    public static String formatMj(long microMj) {
        return MJ_DISPLAY_FORMAT.format(microMj / (double) MJ);
    }

    /** @param side The side of the block at pos that is being connected to. */
    @Nullable
    public static IMjConnector getConnector(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMjConnectorProvider provider) {
            return provider.getMjConnector(side);
        }
        return null;
    }

    /** @param side The side of the block at pos that is being connected to. */
    @Nullable
    public static IMjReceiver getReceiver(BlockGetter level, BlockPos pos, Direction side) {
        return getConnector(level, pos, side) instanceof IMjReceiver receiver ? receiver : null;
    }

    /** @param side The side of the block at pos that is being connected to. */
    @Nullable
    public static IMjPassiveProvider getPassiveProvider(BlockGetter level, BlockPos pos, Direction side) {
        return getConnector(level, pos, side) instanceof IMjPassiveProvider provider ? provider : null;
    }
}
