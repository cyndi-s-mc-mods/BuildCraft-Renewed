package buildcraft.transport.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/** A pipe wire. Right click a pipe to put it in the nearest corner; sneak right click to take it out. */
public class ItemWire extends Item {
    public final DyeColor colour;

    public ItemWire(Properties properties, DyeColor colour) {
        super(properties);
        this.colour = colour;
    }
}
