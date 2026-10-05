package buildcraft.lib;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.item.ICreativeVariants;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryEntry;

public final class BCLib {
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> CREATIVE_TAB;

    private BCLib() {}

    public static void init() {
        BCLibComponents.init();
        buildcraft.lib.misc.ChunkLoader.init();
        CREATIVE_TAB = BCRegistry.register(Registries.CREATIVE_MODE_TAB, "main", key -> Platform.INSTANCE.creativeTabBuilder()
            .title(Component.translatable("itemGroup.buildcraft.main"))
            .icon(() -> new ItemStack(buildcraft.core.BCCoreItems.WRENCH.get()))
            .displayItems((params, output) -> {
                for (Supplier<? extends Item> item : RegistrationHelper.items()) {
                    if (item.get() instanceof ICreativeVariants variants) {
                        variants.addCreativeVariants(output::accept);
                    } else {
                        output.accept(item.get());
                    }
                }
            })
            .build());
    }
}
