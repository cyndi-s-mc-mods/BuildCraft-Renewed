/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.statements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;

/** The registry of every statement and parameter type, and of the providers that say which ones a container can use. */
public final class StatementManager {
    private static final Map<String, IStatement> STATEMENTS = new LinkedHashMap<>();
    private static final List<IStatement> STATEMENT_LIST = new ArrayList<>();
    private static final Map<String, Function<ValueInput, IStatementParameter>> PARAMETERS = new LinkedHashMap<>();
    private static final List<IStatementProvider> PROVIDERS = new ArrayList<>();

    private StatementManager() {}

    public static <S extends IStatement> S registerStatement(S statement) {
        if (STATEMENTS.put(statement.getUniqueTag(), statement) == null) {
            STATEMENT_LIST.add(statement);
        }
        return statement;
    }

    public static void registerParameter(String tag, Function<ValueInput, IStatementParameter> loader) {
        PARAMETERS.put(tag, loader);
    }

    public static void registerProvider(IStatementProvider provider) {
        PROVIDERS.add(provider);
    }

    @Nullable
    public static IStatement getStatement(String tag) {
        return STATEMENTS.get(tag);
    }

    /** @return The statement's position in the registry, which is the same on the client and the server. */
    public static int getStatementIndex(IStatement statement) {
        return STATEMENT_LIST.indexOf(statement);
    }

    @Nullable
    public static IStatement getStatementByIndex(int index) {
        return index >= 0 && index < STATEMENT_LIST.size() ? STATEMENT_LIST.get(index) : null;
    }

    @Nullable
    public static IStatementParameter loadParameter(ValueInput input) {
        Function<ValueInput, IStatementParameter> loader = PARAMETERS.get(input.getStringOr("kind", ""));
        return loader == null ? null : loader.apply(input);
    }

    public static Set<ITriggerInternal> getInternalTriggers(IStatementContainer container) {
        Set<ITriggerInternal> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addInternalTriggers(set, container));
        return set;
    }

    public static Set<ITriggerInternalSided> getInternalSidedTriggers(IStatementContainer container, Direction side) {
        Set<ITriggerInternalSided> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addInternalSidedTriggers(set, container, side));
        return set;
    }

    public static Set<ITriggerExternal> getExternalTriggers(Direction side, BlockEntity tile) {
        Set<ITriggerExternal> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addExternalTriggers(set, side, tile));
        return set;
    }

    public static Set<IActionInternal> getInternalActions(IStatementContainer container) {
        Set<IActionInternal> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addInternalActions(set, container));
        return set;
    }

    public static Set<IActionInternalSided> getInternalSidedActions(IStatementContainer container, Direction side) {
        Set<IActionInternalSided> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addInternalSidedActions(set, container, side));
        return set;
    }

    public static Set<IActionExternal> getExternalActions(Direction side, BlockEntity tile) {
        Set<IActionExternal> set = new LinkedHashSet<>();
        PROVIDERS.forEach(p -> p.addExternalActions(set, side, tile));
        return set;
    }
}
