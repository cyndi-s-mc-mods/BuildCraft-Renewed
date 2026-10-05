/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

import buildcraft.api.recipes.AssemblyRecipes;
import buildcraft.api.recipes.AssemblyRecipes.AssemblyRecipe;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.container.ContainerAssemblyTable;

/** Makes items from the recipes the player has chosen ("saved"), using laser power. */
public class TileAssemblyTable extends TileLaserTableBase implements WorldlyContainer {
    public static final int SLOTS = 12;
    private static final int[] ALL_SLOTS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

    public enum State {
        /** The materials are there, but the player hasn't chosen to make it. */
        POSSIBLE,
        /** Chosen, but the materials aren't all there. */
        SAVED,
        /** Chosen, with all the materials. */
        SAVED_ENOUGH,
        /** Being made right now. */
        SAVED_ENOUGH_ACTIVE
    }

    private static final class Entry {
        final AssemblyRecipe recipe;
        State state;

        Entry(AssemblyRecipe recipe, State state) {
            this.recipe = recipe;
            this.state = state;
        }
    }

    public final ItemHandlerSimple inv = new ItemHandlerSimple(SLOTS, this::setChanged);
    private final List<Entry> entries = new ArrayList<>();
    /** The outputs of the recipes in the list, shown in the GUI. */
    public final SimpleContainer display = new SimpleContainer(SLOTS);

    public TileAssemblyTable(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.ASSEMBLY_TABLE_TILE.get(), pos, state);
    }

    /** @return The state of the recipe shown in the given GUI slot, or -1 for none. */
    public int getDisplayState(int index) {
        return index < entries.size() ? entries.get(index).state.ordinal() : -1;
    }

    /** Called when a player clicks a recipe in the GUI: chooses it, or stops making it. */
    public void toggleRecipe(int index) {
        if (index < 0 || index >= entries.size()) return;
        Entry entry = entries.get(index);
        entry.state = entry.state == State.POSSIBLE ? State.SAVED : State.POSSIBLE;
        updateRecipes();
        setChanged();
    }

    @Nullable
    private Entry getActive() {
        for (Entry entry : entries) {
            if (entry.state == State.SAVED_ENOUGH_ACTIVE) return entry;
        }
        return null;
    }

    private void updateRecipes() {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!inv.getItem(i).isEmpty()) contents.add(inv.getItem(i));
        }
        List<String> order = new ArrayList<>();
        for (AssemblyRecipe recipe : AssemblyRecipes.getAllFor(contents)) {
            order.add(recipe.name());
            if (entries.stream().noneMatch(e -> e.recipe.name().equals(recipe.name())) && extract(inv, recipe.inputs(), true)) {
                entries.add(new Entry(recipe, State.POSSIBLE));
            }
        }
        // Recipes that aren't listed any more (saved ones whose items have gone) go last
        entries.sort((a, b) -> Integer.compare(indexOrMax(order, a.recipe.name()), indexOrMax(order, b.recipe.name())));
        boolean hasActive = false;
        for (var iterator = entries.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            boolean enough = extract(inv, entry.recipe.inputs(), true);
            if (entry.state == State.POSSIBLE) {
                if (!enough) iterator.remove();
            } else if (!enough) {
                entry.state = State.SAVED;
            } else if (entry.state == State.SAVED) {
                entry.state = State.SAVED_ENOUGH;
            }
            if (entry.state == State.SAVED_ENOUGH_ACTIVE) hasActive = true;
        }
        if (!hasActive) {
            for (Entry entry : entries) {
                if (entry.state == State.SAVED_ENOUGH) {
                    entry.state = State.SAVED_ENOUGH_ACTIVE;
                    break;
                }
            }
        }
        for (int i = 0; i < SLOTS; i++) {
            display.setItem(i, i < entries.size() ? entries.get(i).recipe.output().copy() : ItemStack.EMPTY);
        }
    }

    private static int indexOrMax(List<String> order, String name) {
        int index = order.indexOf(name);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    /** Moves on to the next saved recipe that can be made, so several saved recipes take turns. */
    private void activateNext() {
        Entry active = getActive();
        if (active == null) return;
        int index = entries.indexOf(active);
        for (int i = 1; i < entries.size(); i++) {
            Entry next = entries.get((index + i) % entries.size());
            if (next.state == State.SAVED_ENOUGH) {
                active.state = State.SAVED_ENOUGH;
                next.state = State.SAVED_ENOUGH_ACTIVE;
                return;
            }
        }
    }

    @Override
    public long getTarget() {
        Entry active = getActive();
        return active == null ? 0 : active.recipe.powerRequired();
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            updateRecipes();
            Entry active = getActive();
            if (active != null && power >= active.recipe.powerRequired()) {
                if (extract(inv, active.recipe.inputs(), false)) {
                    InventoryUtil.addToBestAcceptor(level, worldPosition, active.recipe.output().copy());
                    power -= active.recipe.powerRequired();
                }
                updateRecipes();
                activateNext();
            }
        }
        // After the recipes are up to date, as this clears the power when there's nothing to make
        super.tick();
    }

    private record SavedEntry(String recipe, int state) {
        static final Codec<SavedEntry> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("recipe").forGetter(SavedEntry::recipe),
            Codec.INT.fieldOf("state").forGetter(SavedEntry::state)).apply(i, SavedEntry::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inv.save(output, "inv");
        List<SavedEntry> saved = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.state != State.POSSIBLE) {
                saved.add(new SavedEntry(entry.recipe.name(), entry.state.ordinal()));
            }
        }
        output.store("recipes", SavedEntry.CODEC.listOf(), saved);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inv.load(input, "inv");
        entries.clear();
        for (SavedEntry saved : input.read("recipes", SavedEntry.CODEC.listOf()).orElse(List.of())) {
            AssemblyRecipe recipe = AssemblyRecipes.get(saved.recipe());
            if (recipe != null && saved.state() >= 0 && saved.state() < State.values().length) {
                entries.add(new Entry(recipe, State.values()[saved.state()]));
            }
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerAssemblyTable(id, inventory, this);
    }

    // WorldlyContainer

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return inv.removeItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inv.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inv.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.stillValid(player);
    }

    @Override
    public void clearContent() {
        inv.clearContent();
    }
}
