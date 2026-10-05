package buildcraft.forge;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;

import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.BuildCraft;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidDefinition;

/** Gives BuildCraft's fluids the FluidType that Forge needs. */
final class ForgeFluids {
    private static final Map<BCFluidDefinition, FluidType> TYPES = new IdentityHashMap<>();

    private ForgeFluids() {}

    static synchronized FluidType typeFor(BCFluidDefinition def) {
        return TYPES.computeIfAbsent(def, d -> new FluidType(FluidType.Properties.create()
            .descriptionId(d.translationKey())
            .density(d.density)
            .viscosity(d.viscosity)
            .temperature(d.temperature)
            .canConvertToSource(false)
            .canSwim(true)
            .canDrown(true)
            .canExtinguish(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    }

    static FlowingFluid create(BCFluidDefinition def, boolean source) {
        return source ? new Source(def) : new Flowing(def);
    }

    static void registerTypes(ResourceKey<? extends Registry<?>> key, Registrar registrar) {
        if (key.equals(ForgeRegistries.Keys.FLUID_TYPES)) {
            for (BCFluidDefinition def : BCFluidDefinition.ALL) {
                registrar.register(def.id, typeFor(def));
            }
        }
    }

    interface Registrar {
        void register(String name, FluidType type);
    }

    static final class Source extends BCFluid.Source {
        Source(BCFluidDefinition def) {
            super(def);
        }

        @Override
        public FluidType getFluidType() {
            return typeFor(def);
        }
    }

    static final class Flowing extends BCFluid.Flowing {
        Flowing(BCFluidDefinition def) {
            super(def);
        }

        @Override
        public FluidType getFluidType() {
            return typeFor(def);
        }
    }
}
