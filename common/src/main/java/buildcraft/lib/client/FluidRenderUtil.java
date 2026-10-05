package buildcraft.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.tags.FluidTags;

/** Finds the textures and colours fluids are drawn with outside of the world, such as in tank GUIs and pipes. */
public final class FluidRenderUtil {
    /** The default water colour, used where there's no biome to tint water by. */
    private static final int WATER_COLOUR = 0xFF3F76E4;

    private FluidRenderUtil() {}

    public static TextureAtlasSprite getStillSprite(Fluid fluid) {
        FluidState state = fluid.defaultFluidState();
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state).stillMaterial().sprite();
    }

    public static TextureAtlasSprite getFlowingSprite(Fluid fluid) {
        FluidState state = fluid.defaultFluidState();
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state).flowingMaterial().sprite();
    }

    /** @return The ARGB colour to tint the fluid's texture with. */
    public static int getColour(Fluid fluid) {
        if (fluid != Fluids.EMPTY && fluid.defaultFluidState().is(FluidTags.WATER)) {
            return WATER_COLOUR;
        }
        return -1;
    }
}
