package buildcraft.lib.statement;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;
import buildcraft.api.statements.StatementManager;

/** A statement placed in a gate slot: which statement it is, which side it looks at (for sided and external ones), and
 * its parameters. */
public final class StatementWrapper {
    public final IStatement statement;
    @Nullable
    public final Direction side;
    public final IStatementParameter[] parameters;

    public StatementWrapper(IStatement statement, @Nullable Direction side, IStatementParameter[] parameters) {
        this.statement = statement;
        this.side = side;
        this.parameters = parameters;
    }

    /** Creates a wrapper with the statement's default parameters.
     * @param paramCount How many parameters the gate allows. */
    public static StatementWrapper create(IStatement statement, @Nullable Direction side, int paramCount) {
        int count = Math.min(paramCount, statement.maxParameters());
        IStatementParameter[] params = new IStatementParameter[count];
        for (int i = 0; i < count; i++) {
            params[i] = statement.createParameter(i);
        }
        return new StatementWrapper(statement, side, params);
    }

    public static boolean needsSide(IStatement statement) {
        return statement instanceof ITriggerInternalSided || statement instanceof ITriggerExternal
            || statement instanceof IActionInternalSided || statement instanceof IActionExternal;
    }

    public Component getDescription() {
        Component desc = statement.getDescription();
        if (side != null) {
            return Component.translatable("gate.buildcraft.side", desc, Component.translatable("direction.buildcraft." + side.getSerializedName()));
        }
        return desc;
    }

    public boolean isTriggerActive(IStatementContainer container) {
        if (statement instanceof ITriggerInternal trigger) {
            return trigger.isTriggerActive(container, parameters);
        }
        if (side != null) {
            if (statement instanceof ITriggerInternalSided trigger) {
                return trigger.isTriggerActive(side, container, parameters);
            }
            if (statement instanceof ITriggerExternal trigger) {
                BlockEntity tile = container.getNeighbourTile(side);
                return tile != null && trigger.isTriggerActive(tile, side.getOpposite(), container, parameters);
            }
        }
        return false;
    }

    public void activate(IStatementContainer container) {
        if (statement instanceof IActionInternal action) {
            action.actionActivate(container, parameters);
        } else if (side != null) {
            if (statement instanceof IActionInternalSided action) {
                action.actionActivate(side, container, parameters);
            } else if (statement instanceof IActionExternal action) {
                BlockEntity tile = container.getNeighbourTile(side);
                if (tile != null) action.actionActivate(tile, side.getOpposite(), container, parameters);
            }
        }
    }

    public void deactivate(IStatementContainer container) {
        if (statement instanceof IActionInternal action) {
            action.actionDeactivated(container, parameters);
        } else if (side != null) {
            if (statement instanceof IActionInternalSided action) {
                action.actionDeactivated(side, container, parameters);
            } else if (statement instanceof IActionExternal action) {
                BlockEntity tile = container.getNeighbourTile(side);
                if (tile != null) action.actionDeactivated(tile, side.getOpposite(), container, parameters);
            }
        }
    }

    public void save(ValueOutput output) {
        output.putString("kind", statement.getUniqueTag());
        if (side != null) {
            output.putString("side", side.getSerializedName());
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] != null) {
                ValueOutput param = output.child("param" + i);
                param.putString("kind", parameters[i].getUniqueTag());
                parameters[i].save(param);
            }
        }
    }

    @Nullable
    public static StatementWrapper load(ValueInput input, int paramCount) {
        IStatement statement = StatementManager.getStatement(input.getStringOr("kind", ""));
        if (statement == null) return null;
        Direction side = Direction.byName(input.getStringOr("side", ""));
        StatementWrapper wrapper = create(statement, side, paramCount);
        for (int i = 0; i < wrapper.parameters.length; i++) {
            int index = i;
            input.child("param" + i).map(StatementManager::loadParameter).ifPresent(p -> wrapper.parameters[index] = p);
        }
        return wrapper;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof StatementWrapper other && other.statement == statement && other.side == side
            && Arrays.equals(other.parameters, parameters);
    }

    @Override
    public int hashCode() {
        return statement.hashCode() * 31 + (side == null ? 0 : side.hashCode());
    }
}
