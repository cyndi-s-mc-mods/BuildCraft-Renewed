/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;


import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tiles.IHasWork;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerAutoWorkbench;
import buildcraft.lib.crafting.WorkbenchCrafting;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.tile.TileBC;

/** Crafts the recipe laid out in its blueprint grid, using the materials put into it. It works slowly by itself and
 * much faster with power. */
public class TileAutoWorkbench extends TileBC implements WorldlyContainer, MenuProvider, IMjConnectorProvider, IHasWork {
    /** A redstone engine makes {@code 1 MJ} per tick, so the workbench is a lot slower without one. */
    private static final long POWER_GEN_PASSIVE = MjAPI.MJ / 5;
    /** It takes 10 seconds to craft an item without power. */
    public static final long POWER_REQUIRED = POWER_GEN_PASSIVE * 20 * 10;
    private static final long POWER_LOST = POWER_GEN_PASSIVE * 10;
    public static final int RESULT_SLOT = 9;
    private static final int[] SLOTS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, RESULT_SLOT };

    public final ItemHandlerSimple invBlueprint = new ItemHandlerSimple(9, this::onBlueprintChanged);
    public final ItemHandlerSimple invMaterials = new ItemHandlerSimple(9, (slot, stack) -> isMaterial(stack), this::setChanged);
    public final ItemHandlerSimple invResult = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);
    private final WorkbenchCrafting crafting = new WorkbenchCrafting(invBlueprint, invMaterials, invResult);
    /** What the blueprint makes, shown in the GUI. */
    public final SimpleContainer preview = crafting.preview;

    private long powerStored = 0;
    private final IMjRedstoneReceiver receiver = new Receiver();

    public TileAutoWorkbench(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.AUTO_WORKBENCH_TILE.get(), pos, state);
    }

    private void onBlueprintChanged() {
        crafting.onBlueprintChanged();
        setChanged();
    }

    private boolean isMaterial(ItemStack stack) {
        return crafting.isMaterial(stack);
    }

    public long getPowerStored() {
        return powerStored;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        crafting.tick(server);
        if (crafting.canCraft()) {
            if (powerStored >= POWER_REQUIRED) {
                crafting.craft(server, worldPosition);
                powerStored = crafting.canCraft() ? 1 : 0;
            } else {
                powerStored += POWER_GEN_PASSIVE;
            }
        } else if (powerStored >= POWER_LOST) {
            powerStored -= POWER_LOST;
        } else {
            powerStored = 0;
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
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invBlueprint.save(output, "blueprint");
        invMaterials.save(output, "materials");
        invResult.save(output, "result");
        output.putLong("power", powerStored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invBlueprint.load(input, "blueprint");
        invMaterials.load(input, "materials");
        invResult.load(input, "result");
        powerStored = input.getLongOr("power", 0);
        crafting.markDirty();
    }

    private final class Receiver implements IMjRedstoneReceiver {
        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return Math.max(0, POWER_REQUIRED - powerStored);
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            long taken = Math.min(getPowerRequested(), microJoules);
            if (!simulate) {
                powerStored += taken;
            }
            return microJoules - taken;
        }
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerAutoWorkbench(id, inventory, this);
    }

    // WorldlyContainer: materials go in, the result comes out

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < RESULT_SLOT && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESULT_SLOT;
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        return invMaterials.isEmpty() && invResult.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == RESULT_SLOT ? invResult.getItem(0) : invMaterials.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return slot == RESULT_SLOT ? invResult.removeItem(0, count) : invMaterials.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == RESULT_SLOT ? invResult.removeItemNoUpdate(0) : invMaterials.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == RESULT_SLOT) {
            invResult.setItem(0, stack);
        } else {
            invMaterials.setItem(slot, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < RESULT_SLOT && invMaterials.canPlaceItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return invMaterials.stillValid(player);
    }

    @Override
    public void clearContent() {
        invMaterials.clearContent();
        invResult.clearContent();
    }

    @Override
    public boolean hasWork() {
        return powerStored > 0;
    }
}
