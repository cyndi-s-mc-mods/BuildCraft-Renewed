/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.core.BCCoreMenus;
import buildcraft.core.item.ItemList;
import buildcraft.lib.BCLibComponents;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotPhantom;
import buildcraft.lib.list.ListData;

/** Edits the list in the player's hand. The items are phantom slots (synced like normal slots), and each line's options
 * are buttons. */
public class ContainerList extends ContainerBC<BlockEntity> {
    @Nullable
    private final InteractionHand hand;
    private final SimpleContainer items = new SimpleContainer(ListData.WIDTH * ListData.HEIGHT) {
        @Override
        public void setChanged() {
            super.setChanged();
            save();
        }
    };
    /** For each line: precise, by type, by material. */
    public final MenuData.Field[] options = new MenuData.Field[ListData.HEIGHT * 3];
    private boolean loading = false;

    /** Client constructor. */
    public ContainerList(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerList(int id, Inventory inventory, @Nullable InteractionHand hand) {
        super(BCCoreMenus.LIST.get(), id, inventory, null);
        this.hand = hand;
        for (int i = 0; i < options.length; i++) {
            int line = i / 3, option = i % 3;
            options[i] = data.addBoolean(hand == null ? null : () -> {
                ListData.Line l = getData().lines().get(line);
                return option == 0 ? l.precise() : option == 1 ? l.byType() : l.byMaterial();
            });
        }
        if (hand != null) {
            loading = true;
            ListData list = getData();
            for (int line = 0; line < ListData.HEIGHT; line++) {
                for (int i = 0; i < ListData.WIDTH; i++) {
                    items.setItem(line * ListData.WIDTH + i, list.lines().get(line).stacks().get(i));
                }
            }
            loading = false;
        }
        for (int line = 0; line < ListData.HEIGHT; line++) {
            for (int i = 0; i < ListData.WIDTH; i++) {
                addSlot(new SlotPhantom(items, line * ListData.WIDTH + i, 8 + i * 18, 32 + line * 34));
            }
        }
        addPlayerInventory(103);
    }

    private ItemStack getList() {
        return hand == null ? ItemStack.EMPTY : playerInventory.player.getItemInHand(hand);
    }

    private ListData getData() {
        return ItemList.getData(getList());
    }

    private void save() {
        ItemStack list = getList();
        if (loading || !(list.getItem() instanceof ItemList)) return;
        ListData data = getData();
        for (int line = 0; line < ListData.HEIGHT; line++) {
            ListData.Line l = data.lines().get(line);
            for (int i = 0; i < ListData.WIDTH; i++) {
                l = l.withStack(i, items.getItem(line * ListData.WIDTH + i));
            }
            data = data.withLine(line, l);
        }
        list.set(BCLibComponents.LIST.get(), data);
    }

    public boolean getOption(int line, int option) {
        return options[line * 3 + option].getBoolean();
    }

    public boolean isOneStackMode(int line) {
        return getOption(line, 1) || getOption(line, 2);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        // In one item mode, only the first item of the line can be set
        if (slotIndex >= 0 && slotIndex < ListData.WIDTH * ListData.HEIGHT && slotIndex % ListData.WIDTH != 0
            && isOneStackMode(slotIndex / ListData.WIDTH)) {
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    /** Buttons: line * 3 + option (0 precise, 1 by type, 2 by material). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (hand == null || id < 0 || id >= ListData.HEIGHT * 3) return false;
        int line = id / 3, option = id % 3;
        ListData data = getData();
        ListData.Line toggled = data.lines().get(line).toggle(option);
        getList().set(BCLibComponents.LIST.get(), data.withLine(line, toggled));
        loading = true;
        for (int i = 0; i < ListData.WIDTH; i++) {
            items.setItem(line * ListData.WIDTH + i, toggled.stacks().get(i));
        }
        loading = false;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return hand == null || player.getItemInHand(hand).getItem() instanceof ItemList;
    }
}
