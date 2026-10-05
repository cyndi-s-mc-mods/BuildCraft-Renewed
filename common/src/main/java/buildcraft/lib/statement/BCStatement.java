package buildcraft.lib.statement;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import buildcraft.BuildCraft;
import buildcraft.api.statements.IStatement;

/** A statement with a fixed name, description and icon. */
public abstract class BCStatement implements IStatement {
    private final String tag;
    private final String descriptionKey;
    private final Identifier icon;

    /** @param name The name, such as "redstone_input_active". The tag is "buildcraft:" + name.
     * @param icon The name of the icon in textures/gui/triggers, such as "trigger_redstoneinput_active". */
    protected BCStatement(String name, String descriptionKey, String icon) {
        this.tag = BuildCraft.MOD_ID + ":" + name;
        this.descriptionKey = descriptionKey;
        this.icon = BuildCraft.id("textures/gui/triggers/" + icon + ".png");
    }

    @Override
    public String getUniqueTag() {
        return tag;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }

    @Override
    public Identifier getIcon() {
        return icon;
    }
}
