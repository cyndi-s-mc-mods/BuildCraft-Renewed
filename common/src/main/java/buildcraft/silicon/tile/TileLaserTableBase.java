/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

import buildcraft.api.power.ILaserTarget;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.tile.TileBC;

/** A table that is powered by lasers. */
public abstract class TileLaserTableBase extends TileBC implements ILaserTarget, MenuProvider {
    public long power;

    protected TileLaserTableBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** @return The power needed to finish the current job, or 0 if there's nothing to do. */
    public abstract long getTarget();

    @Override
    public long getRequiredLaserPower() {
        return Math.max(0, getTarget() - power);
    }

    @Override
    public long receiveLaserPower(long microJoules) {
        long received = Math.min(microJoules, getRequiredLaserPower());
        power += received;
        if (received > 0) setChanged();
        return microJoules - received;
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide() && getTarget() <= 0) {
            power = 0;
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!player.level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0);
    }

    /** Takes (or checks for) the given ingredients from the inventory.
     * @return True if all of them were there. */
    protected static boolean extract(ItemHandlerSimple inv, List<IngredientStack> items, boolean simulate) {
        int[] used = new int[inv.getContainerSize()];
        for (IngredientStack ingredient : items) {
            int remaining = ingredient.count();
            for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getCount() - used[i] <= 0 || !ingredient.test(stack)) continue;
                int spend = Math.min(remaining, stack.getCount() - used[i]);
                remaining -= spend;
                used[i] += spend;
            }
            if (remaining > 0) return false;
        }
        if (!simulate) {
            for (int i = 0; i < used.length; i++) {
                if (used[i] > 0) inv.removeItem(i, used[i]);
            }
        }
        return true;
    }
}
