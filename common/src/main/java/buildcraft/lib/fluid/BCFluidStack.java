package buildcraft.lib.fluid;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** An amount of a fluid, in millibuckets (1000 per bucket). Always uses the source form of a fluid. Immutable. */
public final class BCFluidStack {
    public static final int BUCKET = 1000;
    public static final BCFluidStack EMPTY = new BCFluidStack(Fluids.EMPTY, 0);
    public static final Codec<BCFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(BCFluidStack::getFluid),
        Codec.INT.fieldOf("amount").forGetter(BCFluidStack::getAmount)).apply(instance, BCFluidStack::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, BCFluidStack> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.FLUID), BCFluidStack::getFluid, ByteBufCodecs.VAR_INT, BCFluidStack::getAmount, BCFluidStack::of);

    private final Fluid fluid;
    private final int amount;

    private BCFluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public static BCFluidStack of(Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY || amount <= 0) return EMPTY;
        if (fluid instanceof FlowingFluid flowing) {
            fluid = flowing.getSource();
        }
        return new BCFluidStack(fluid, amount);
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }

    public BCFluidStack withAmount(int newAmount) {
        return of(fluid, newAmount);
    }

    public boolean isSameFluid(BCFluidStack other) {
        return fluid == other.fluid;
    }

    public boolean isSameFluid(Fluid other) {
        return fluid == other || (other instanceof FlowingFluid flowing && flowing.getSource() == fluid);
    }

    public Component getName() {
        if (fluid.defaultFluidState().createLegacyBlock().getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
            return fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
        }
        return Component.literal(BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    public void save(ValueOutput output) {
        if (isEmpty()) return;
        output.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
        output.putInt("amount", amount);
    }

    public static BCFluidStack load(ValueInput input) {
        Optional<String> name = input.getString("fluid");
        if (name.isEmpty()) return EMPTY;
        Identifier id = Identifier.tryParse(name.get());
        if (id == null) return EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
        return of(fluid, input.getIntOr("amount", 0));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof BCFluidStack other && other.fluid == fluid && other.amount == amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fluid, amount);
    }

    @Override
    public String toString() {
        return isEmpty() ? "empty" : amount + "mB of " + BuiltInRegistries.FLUID.getKey(fluid);
    }
}
