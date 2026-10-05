/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlocks;
import buildcraft.lib.tile.TileBC;

/** Takes all the power it's given, and tells the player how much it got when they right click it. */
public class TilePowerTester extends TileBC implements IMjConnectorProvider, IMjReceiver {
    private long lastReceived;
    private long nextTickReceived;
    private long lastTickReceived;
    private long totalReceived;

    public TilePowerTester(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.POWER_TESTER_TILE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        lastTickReceived = nextTickReceived;
        nextTickReceived = 0;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return this;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return 100_000 * MjAPI.MJ;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (!simulate) {
            lastReceived = microJoules;
            nextTickReceived += microJoules;
            totalReceived += microJoules;
            setChanged();
        }
        return 0;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.sendSystemMessage(Component.translatable("chat.buildcraft.power_tester", mj(lastReceived), mj(lastTickReceived),
                mj(totalReceived)));
        }
        return InteractionResult.SUCCESS;
    }

    private static String mj(long microJoules) {
        return String.format(Locale.ROOT, "%.2f", microJoules / (double) MjAPI.MJ);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("last", lastReceived);
        output.putLong("lt", lastTickReceived);
        output.putLong("total", totalReceived);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lastReceived = input.getLongOr("last", 0);
        lastTickReceived = input.getLongOr("lt", 0);
        totalReceived = input.getLongOr("total", 0);
    }
}
