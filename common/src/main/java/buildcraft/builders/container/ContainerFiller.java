/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.filler.FillerParameters;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.builders.tile.TileFiller;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;

public class ContainerFiller extends ContainerBC<TileFiller> {
    public static final int BUTTON_EXCAVATE = 0;
    public static final int BUTTON_INVERT = 1;
    /** Plus the index of the pattern in {@link Patterns#ALL}. */
    public static final int BUTTON_PATTERN = 100;
    /** Plus the parameter index * 2, plus the mouse button. */
    public static final int BUTTON_PARAM = 200;

    public final MenuData.Field pattern;
    public final MenuData.Field[] params = new MenuData.Field[TileFiller.PARAM_COUNT];
    public final MenuData.Field excavate;
    public final MenuData.Field inverted;
    public final MenuData.Field locked;
    public final MenuData.Field finished;
    public final MenuData.Field mode;
    public final MenuData.Field toBreak;
    public final MenuData.Field toPlace;
    public final MenuData.Field power;
    public final MenuData.Field hasBox;

    /** Client constructor. */
    public ContainerFiller(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerFiller(int id, Inventory inventory, @Nullable TileFiller tile) {
        super(BCBuildersMenus.FILLER.get(), id, inventory, tile);
        pattern = data.addInt(tile == null ? null : () -> Patterns.ALL.indexOf(tile.getPattern()));
        for (int i = 0; i < params.length; i++) {
            int index = i;
            params[i] = data.addInt(tile == null ? null : () -> FillerParameters.toId(tile.getParams()[index]));
        }
        excavate = data.addBoolean(tile == null ? null : tile::canExcavate);
        inverted = data.addBoolean(tile == null ? null : tile::isInverted);
        locked = data.addBoolean(tile == null ? null : tile::isLocked);
        finished = data.addBoolean(tile == null ? null : tile::isFinished);
        mode = data.addInt(tile == null ? null : () -> tile.getControlMode().ordinal());
        toBreak = data.addInt(tile == null ? null : tile::getLeftToBreak);
        toPlace = data.addInt(tile == null ? null : tile::getLeftToPlace);
        power = data.addLong(tile == null ? null : tile::getStoredPower);
        hasBox = data.addBoolean(tile == null ? null : () -> tile.getBox() != null);
        Container inv = tile != null ? tile.inv : new SimpleContainer(TileFiller.INV_SIZE);
        for (int i = 0; i < TileFiller.INV_SIZE; i++) {
            addSlot(new Slot(inv, i, 8 + (i % 9) * 18, 85 + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BlockItem;
                }
            });
        }
        addPlayerInventory(153);
    }

    public Pattern getPattern() {
        int index = pattern.getInt();
        return index >= 0 && index < Patterns.ALL.size() ? Patterns.ALL.get(index) : Patterns.NONE;
    }

    @Nullable
    public IStatementParameter getParam(int index) {
        return FillerParameters.fromId(params[index].getInt());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (tile == null) return false;
        if (id == BUTTON_EXCAVATE) {
            tile.toggleExcavate();
            return true;
        }
        if (id == BUTTON_INVERT) {
            tile.toggleInverted();
            return true;
        }
        if (tile.isLocked()) return false;
        if (id >= BUTTON_PATTERN && id < BUTTON_PATTERN + Patterns.ALL.size()) {
            tile.setPattern(Patterns.ALL.get(id - BUTTON_PATTERN));
            return true;
        }
        int param = (id - BUTTON_PARAM) / 2, button = (id - BUTTON_PARAM) % 2;
        if (id >= BUTTON_PARAM && param < tile.getPattern().maxParameters() && param < TileFiller.PARAM_COUNT) {
            IStatementParameter current = tile.getParams()[param];
            if (current == null) current = tile.getPattern().createParameter(param);
            if (current != null) {
                tile.setParam(param, current.onClick(null, tile.getPattern(), getCarried(), button));
            }
            return true;
        }
        return false;
    }
}
