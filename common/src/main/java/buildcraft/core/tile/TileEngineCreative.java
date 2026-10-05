/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tools.IToolWrench;
import buildcraft.core.BCCoreBlocks;
import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase;

/** Creates power from nothing. Sneak + wrench it to change the output. */
public class TileEngineCreative extends TileEngineBase {
    public static final long[] OUTPUTS = { 1, 2, 4, 8, 16, 32, 64, 128, 256 };
    public int currentOutputIndex = 0;

    public TileEngineCreative(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.ENGINE_CREATIVE_TILE.get(), pos, state);
    }

    @Override
    protected void engineUpdate() {
        if (isBurning()) {
            power += getCurrentOutput();
            long max = getMaxPower();
            if (power > max) {
                power = max;
            }
        } else {
            power = 0;
        }
    }

    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return isRedstonePowered;
    }

    @Override
    public double getPistonSpeed() {
        final double max = 0.08;
        final double min = 0.01;
        double interp = currentOutputIndex / (double) (OUTPUTS.length - 1);
        return min + (max - min) * interp;
    }

    @Override
    protected EnumPowerStage computePowerStage() {
        return EnumPowerStage.BLACK;
    }

    @Override
    public long getMaxPower() {
        return getCurrentOutput() * 10_000;
    }

    @Override
    public long maxPowerReceived() {
        return 2_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 20 * getCurrentOutput();
    }

    @Override
    public float explosionRange() {
        return 0;
    }

    @Override
    public long getCurrentOutput() {
        return OUTPUTS[Mth.clamp(currentOutputIndex, 0, OUTPUTS.length - 1)] * MjAPI.MJ;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!held.isEmpty() && held.getItem() instanceof IToolWrench && player.isSecondaryUseActive()) {
            if (!isClient()) {
                currentOutputIndex = (currentOutputIndex + 1) % OUTPUTS.length;
                player.sendOverlayMessage(Component.translatable("chat.buildcraft.engine.creative.mode", OUTPUTS[currentOutputIndex]));
                sendNetworkUpdate();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("currentOutputIndex", currentOutputIndex);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        currentOutputIndex = Mth.clamp(input.getIntOr("currentOutputIndex", 0), 0, OUTPUTS.length - 1);
    }
}
