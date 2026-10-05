package buildcraft.silicon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistryEntry;
import buildcraft.silicon.gate.GateVariant;

public final class BCSiliconComponents {
    public static RegistryEntry<DataComponentType<?>, DataComponentType<GateVariant>> GATE_VARIANT;

    private BCSiliconComponents() {}

    static void init() {
        GATE_VARIANT = BCRegistry.register(Registries.DATA_COMPONENT_TYPE, "gate_variant",
            key -> DataComponentType.<GateVariant> builder().persistent(GateVariant.CODEC).networkSynchronized(GateVariant.STREAM_CODEC)
                .build());
    }
}
