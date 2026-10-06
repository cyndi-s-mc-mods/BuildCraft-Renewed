/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.addon;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.container.ContainerFillerPlanner;
import buildcraft.builders.filler.FilledTemplate;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.core.marker.VolumeBoxAddon;
import buildcraft.core.marker.VolumeBoxEntity;

/** Plans a filler pattern in a volume box, showing where its blocks would go. A filler placed next to the box builds the
 * planned pattern. */
public class AddonFillerPlanner extends VolumeBoxAddon {
    public static final Identifier TYPE = BuildCraft.id("filler_planner");
    public static final int PARAM_COUNT = 4;

    private Pattern pattern = Patterns.BOX;
    private final @Nullable IStatementParameter[] params = new IStatementParameter[PARAM_COUNT];
    private boolean inverted = false;
    private boolean @Nullable [] preview;

    public AddonFillerPlanner() {
        resetParams();
    }

    @Override
    public Identifier getType() {
        return TYPE;
    }

    @Override
    public Item getItem() {
        return BCBuildersItems.FILLER_PLANNER.get();
    }

    @Override
    public Identifier getSprite() {
        return BuildCraft.id("block/addons/filler_planner");
    }

    public Pattern getPattern() {
        return pattern;
    }

    public @Nullable IStatementParameter[] getParams() {
        return params;
    }

    public boolean isInverted() {
        return inverted;
    }

    private void resetParams() {
        for (int i = 0; i < PARAM_COUNT; i++) {
            params[i] = pattern.createParameter(i);
        }
    }

    public void setPattern(VolumeBoxEntity box, Pattern pattern) {
        if (this.pattern == pattern) return;
        this.pattern = pattern;
        resetParams();
        changed(box);
    }

    public void setParam(VolumeBoxEntity box, int index, @Nullable IStatementParameter param) {
        if (index < 0 || index >= PARAM_COUNT) return;
        params[index] = param;
        changed(box);
    }

    public void toggleInverted(VolumeBoxEntity box) {
        inverted = !inverted;
        changed(box);
    }

    private void changed(VolumeBoxEntity box) {
        onBoxChanged(box);
        box.addonsChanged();
    }

    /** @return Which positions in the area should be filled (indexed as {@link VolumeBoxEntity#indexOf}), or null if
     *         the pattern can't be made in an area of that size. */
    public static boolean @Nullable [] makeTemplate(BoundingBox box, Pattern pattern, @Nullable IStatementParameter[] params,
        boolean inverted) {
        FilledTemplate template = new FilledTemplate(box.getXSpan(), box.getYSpan(), box.getZSpan());
        IStatementParameter[] used = Arrays.copyOf(params, pattern.maxParameters());
        for (int i = 0; i < used.length; i++) {
            if (used[i] == null) used[i] = pattern.createParameter(i);
        }
        if (!pattern.fillTemplate(template, used)) return null;
        if (inverted) template.invert();
        boolean[] filled = new boolean[box.getXSpan() * box.getYSpan() * box.getZSpan()];
        for (int i = 0; i < filled.length; i++) {
            filled[i] = template.get(i);
        }
        return filled;
    }

    @Override
    public void onBoxChanged(VolumeBoxEntity box) {
        preview = makeTemplate(box.getBox(), pattern, params, inverted);
    }

    @Override
    public boolean @Nullable [] getPreview(VolumeBoxEntity box) {
        return preview;
    }

    @Override
    public void onRightClick(VolumeBoxEntity box, ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ContainerFillerPlanner(id, inventory, box, this),
            Component.translatable("item.buildcraft.filler_planner")));
    }

    @Override
    public void save(ValueOutput output) {
        output.putString("pattern", pattern.getUniqueTag());
        for (int i = 0; i < PARAM_COUNT; i++) {
            IStatementParameter param = params[i];
            if (param != null) {
                ValueOutput child = output.child("param" + i);
                child.putString("kind", param.getUniqueTag());
                param.save(child);
            }
        }
        output.putBoolean("inverted", inverted);
    }

    @Override
    public void load(ValueInput input) {
        IStatement statement = StatementManager.getStatement(input.getStringOr("pattern", ""));
        pattern = statement instanceof Pattern p ? p : Patterns.BOX;
        for (int i = 0; i < PARAM_COUNT; i++) {
            params[i] = input.child("param" + i).map(StatementManager::loadParameter).orElse(null);
            if (params[i] == null) params[i] = pattern.createParameter(i);
        }
        inverted = input.getBooleanOr("inverted", false);
    }
}
