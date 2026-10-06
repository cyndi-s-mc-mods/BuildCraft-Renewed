/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.addon.AddonFillerPlanner;
import buildcraft.builders.filler.FillerParameters;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.core.marker.VolumeBoxEntity;
import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;

/** The filler planner's settings: the pattern, its parameters and whether it's inverted. Uses the same buttons as the
 * filler's menu ({@link ContainerFiller}). */
public class ContainerFillerPlanner extends ContainerBC<BlockEntity> {
    private final @Nullable VolumeBoxEntity box;
    private final @Nullable AddonFillerPlanner addon;

    public final MenuData.Field pattern;
    public final MenuData.Field[] params = new MenuData.Field[AddonFillerPlanner.PARAM_COUNT];
    public final MenuData.Field inverted;

    /** Client constructor. */
    public ContainerFillerPlanner(int id, Inventory inventory) {
        this(id, inventory, null, null);
    }

    public ContainerFillerPlanner(int id, Inventory inventory, @Nullable VolumeBoxEntity box, @Nullable AddonFillerPlanner addon) {
        super(BCBuildersMenus.FILLER_PLANNER.get(), id, inventory, null);
        this.box = box;
        this.addon = addon;
        pattern = data.addInt(addon == null ? null : () -> Patterns.ALL.indexOf(addon.getPattern()));
        for (int i = 0; i < params.length; i++) {
            int index = i;
            params[i] = data.addInt(addon == null ? null : () -> FillerParameters.toId(addon.getParams()[index]));
        }
        inverted = data.addBoolean(addon == null ? null : addon::isInverted);
        addDataSlots(data);
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
    public boolean stillValid(Player player) {
        if (box == null || addon == null) return true;
        return box.isAlive() && box.getAddon(AddonFillerPlanner.class) == addon && !box.isLocked()
            && box.getBoundingBox().inflate(8).contains(player.position());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (box == null || addon == null || !stillValid(player)) return false;
        if (id == ContainerFiller.BUTTON_INVERT) {
            addon.toggleInverted(box);
            return true;
        }
        if (id >= ContainerFiller.BUTTON_PATTERN && id < ContainerFiller.BUTTON_PATTERN + Patterns.ALL.size()) {
            addon.setPattern(box, Patterns.ALL.get(id - ContainerFiller.BUTTON_PATTERN));
            return true;
        }
        int param = (id - ContainerFiller.BUTTON_PARAM) / 2, button = (id - ContainerFiller.BUTTON_PARAM) % 2;
        if (id >= ContainerFiller.BUTTON_PARAM && param < addon.getPattern().maxParameters() && param < AddonFillerPlanner.PARAM_COUNT) {
            IStatementParameter current = addon.getParams()[param];
            if (current == null) current = addon.getPattern().createParameter(param);
            if (current != null) {
                addon.setParam(box, param, current.onClick(null, addon.getPattern(), getCarried(), button));
            }
            return true;
        }
        return false;
    }
}
