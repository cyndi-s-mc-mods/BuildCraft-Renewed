/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.tools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

/** Implemented by items that work as a wrench on BuildCraft blocks. */
public interface IToolWrench {
    /** @return True if the wrench can be used right now. */
    boolean canWrench(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace);

    /** Called after the wrench has been used, for effects such as sounds and durability. */
    void wrenchUsed(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace);
}
