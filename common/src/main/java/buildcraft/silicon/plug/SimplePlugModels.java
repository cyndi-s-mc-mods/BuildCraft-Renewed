package buildcraft.silicon.plug;

import net.minecraft.resources.Identifier;

import buildcraft.api.transport.pluggable.PlugModelPart;

/** Shared model shapes for the small silicon pluggables. */
final class SimplePlugModels {
    private SimplePlugModels() {}

    /** The flat 6x6 plate the light sensor, timer and pulsar are built on. */
    static PlugModelPart plate(Identifier texture, float sideU) {
        PlugModelPart.Face side = new PlugModelPart.Face(texture, sideU, 5, 5, 11);
        PlugModelPart.Face end = new PlugModelPart.Face(texture, 5, 5, 11, 11);
        return PlugModelPart.box(2, 5, 5, 4.01f, 11, 11, new PlugModelPart.Face[] { side, side, side, side, end, end });
    }
}
