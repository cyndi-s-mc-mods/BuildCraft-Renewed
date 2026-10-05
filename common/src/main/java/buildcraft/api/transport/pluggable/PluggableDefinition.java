/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pluggable;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;

import buildcraft.api.transport.pipe.IPipeHolder;

/** A kind of pluggable. Every pluggable type must be registered here so that it can be loaded from saved data. */
public final class PluggableDefinition {
    private static final Map<Identifier, PluggableDefinition> REGISTRY = new LinkedHashMap<>();

    @FunctionalInterface
    public interface Loader {
        PipePluggable load(PluggableDefinition definition, IPipeHolder holder, Direction side, ValueInput input);
    }

    @FunctionalInterface
    public interface Creator {
        PipePluggable create(PluggableDefinition definition, IPipeHolder holder, Direction side);
    }

    public final Identifier id;
    public final Loader loader;

    public PluggableDefinition(Identifier id, Loader loader) {
        this.id = id;
        this.loader = loader;
    }

    /** For pluggables that don't save any data of their own. */
    public PluggableDefinition(Identifier id, Creator creator) {
        this(id, (def, holder, side, input) -> creator.create(def, holder, side));
    }

    public static PluggableDefinition register(PluggableDefinition definition) {
        REGISTRY.put(definition.id, definition);
        return definition;
    }

    @Nullable
    public static PluggableDefinition get(Identifier id) {
        return REGISTRY.get(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
