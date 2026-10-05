package buildcraft.lib.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import buildcraft.lib.registry.RegistryEntry;

/** Everything about one of BuildCraft's fluids. The loader turns this into Fluid (and FluidType) instances. */
public final class BCFluidDefinition {
    /** Every fluid BuildCraft adds, in registration order. */
    public static final List<BCFluidDefinition> ALL = new ArrayList<>();

    /** Registry name of the source fluid, block and (with "_bucket") bucket, such as "oil_heat_1". */
    public final String id;
    /** The base name shared by all heat levels, such as "oil". Used for the translation. */
    public final String baseName;
    public final int heat;
    public final int lightColour, darkColour;
    /** In kg/m^3, negative for gases. */
    public final int density;
    public final int viscosity;
    /** In kelvin. */
    public final int temperature;
    /** How far the fluid flows, from 1 to 16 blocks. */
    public final int spread;
    public final boolean flammable;

    public RegistryEntry<Fluid, FlowingFluid> source;
    public RegistryEntry<Fluid, FlowingFluid> flowing;
    public RegistryEntry<Block, LiquidBlock> block;
    public RegistryEntry<Item, BucketItem> bucket;

    /** Loader-specific data, such as a Forge or NeoForge FluidType. */
    public Object platformData;

    public BCFluidDefinition(String baseName, int heat, int density, int viscosity, int boilPoint, int spread,
        int lightColour, int darkColour, boolean flammable) {
        this.baseName = baseName;
        this.heat = heat;
        this.id = heat == 0 ? baseName : baseName + "_heat_" + heat;
        this.viscosity = viscosity * (4 - heat) / 4;
        this.density = density * (heat >= boilPoint ? -1 : 1);
        this.temperature = 300 + 20 * heat;
        this.spread = spread;
        this.lightColour = lightColour;
        this.darkColour = darkColour;
        this.flammable = flammable;
    }

    public String translationKey() {
        return "block.buildcraft." + id;
    }

    public Component getName() {
        return Component.translatable(translationKey());
    }

    public boolean isGaseous() {
        return density < 0;
    }

    public String stillTexture() {
        return "block/fluids/" + baseName + "_heat_" + heat + "_still";
    }

    public String flowTexture() {
        return "block/fluids/" + baseName + "_heat_" + heat + "_flow";
    }
}
