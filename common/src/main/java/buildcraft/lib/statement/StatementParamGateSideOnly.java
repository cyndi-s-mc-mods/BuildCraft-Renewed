package buildcraft.lib.statement;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

/** Toggles whether a redstone statement only looks at (or only outputs from) the gate's own side. */
public final class StatementParamGateSideOnly implements IStatementParameter {
    public static final String TAG = "buildcraft:gate_side_only";
    public static final StatementParamGateSideOnly ANY = new StatementParamGateSideOnly(false);
    public static final StatementParamGateSideOnly SIDE_ONLY = new StatementParamGateSideOnly(true);
    private static final Identifier ICON = BuildCraft.id("textures/gui/triggers/redstone_gate_side_only.png");

    public final boolean sideOnly;

    private StatementParamGateSideOnly(boolean sideOnly) {
        this.sideOnly = sideOnly;
    }

    public static StatementParamGateSideOnly load(ValueInput input) {
        return input.getBooleanOr("sideOnly", false) ? SIDE_ONLY : ANY;
    }

    public static boolean isSideOnly(IStatementParameter[] params) {
        return params.length > 0 && params[0] instanceof StatementParamGateSideOnly p && p.sideOnly;
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }

    @Override
    public ItemStack getItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable Identifier getIcon() {
        return sideOnly ? ICON : null;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(sideOnly ? "gate.buildcraft.parameter.gate_side_only" : "gate.buildcraft.parameter.all_sides");
    }

    @Override
    public IStatementParameter onClick(IStatementContainer source, IStatement statement, ItemStack held, int button) {
        return sideOnly ? ANY : SIDE_ONLY;
    }

    @Override
    public void save(ValueOutput output) {
        output.putBoolean("sideOnly", sideOnly);
    }
}
