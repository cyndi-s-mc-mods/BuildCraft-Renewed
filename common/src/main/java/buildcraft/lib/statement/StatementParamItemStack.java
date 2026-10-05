package buildcraft.lib.statement;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

/** An item, chosen by clicking the parameter with it. Clicking with an empty hand clears it. */
public final class StatementParamItemStack implements IStatementParameter {
    public static final String TAG = "buildcraft:item_stack";
    public static final StatementParamItemStack EMPTY = new StatementParamItemStack(ItemStack.EMPTY);

    public final ItemStack stack;

    public StatementParamItemStack(ItemStack stack) {
        this.stack = stack;
    }

    public static StatementParamItemStack load(ValueInput input) {
        return new StatementParamItemStack(input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public String getUniqueTag() {
        return TAG;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

    @Override
    public Component getDescription() {
        return stack.isEmpty() ? Component.translatable("gate.buildcraft.parameter.item.empty") : stack.getHoverName();
    }

    @Override
    public IStatementParameter onClick(IStatementContainer source, IStatement statement, ItemStack held, int button) {
        return held.isEmpty() ? EMPTY : new StatementParamItemStack(held.copyWithCount(1));
    }

    @Override
    public void save(ValueOutput output) {
        if (!stack.isEmpty()) {
            output.store("item", ItemStack.CODEC, stack);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StatementParamItemStack other && ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        return stack.getItem().hashCode();
    }
}
