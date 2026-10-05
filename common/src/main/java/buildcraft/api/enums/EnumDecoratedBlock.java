/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.enums;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/** Plain decorative blocks, using BuildCraft's textures. */
public enum EnumDecoratedBlock implements StringRepresentable {
    DESTROY(0),
    BLUEPRINT(10),
    TEMPLATE(10),
    LASER_BACK(0);

    public final int lightValue;

    EnumDecoratedBlock(int lightValue) {
        this.lightValue = lightValue;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
