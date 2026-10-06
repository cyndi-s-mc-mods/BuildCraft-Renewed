package buildcraft.silicon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;

import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.FacadeData;

public final class BCSiliconComponents {
    public static RegistryEntry<DataComponentType<?>, DataComponentType<GateVariant>> GATE_VARIANT;
    public static RegistryEntry<DataComponentType<?>, DataComponentType<FacadeData>> FACADE;

    /** The gate settings a gate copier holds. */
    public static RegistryEntry<DataComponentType<?>, DataComponentType<CompoundTag>> GATE_COPY;

    private BCSiliconComponents() {}

    static void init() {
        GATE_VARIANT = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "gate_variant",
            key -> DataComponentType.<GateVariant> builder().persistent(GateVariant.CODEC).networkSynchronized(GateVariant.STREAM_CODEC)
                .build());
        GATE_COPY = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "gate_copy",
            key -> DataComponentType.<CompoundTag> builder().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                .build());
        FACADE = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "facade",
            key -> DataComponentType.<FacadeData> builder().persistent(FacadeData.CODEC).networkSynchronized(FacadeData.STREAM_CODEC)
                .build());
    }
}
