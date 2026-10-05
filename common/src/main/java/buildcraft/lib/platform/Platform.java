package buildcraft.lib.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The loader-specific parts of BuildCraft. Each loader provides one implementation through {@link ServiceLoader}. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
        .orElseThrow(() -> new IllegalStateException("No BuildCraft platform implementation found"));

    String loaderName();

    Path configDir();

    CreativeModeTab.Builder creativeTabBuilder();

    /** Lets the loader adjust furnace burn times (Forge fires an event for this).
     * @param vanillaBurnTime The burn time from the item's cooking fuel component. */
    default int getBurnTime(ItemStack stack, int vanillaBurnTime) {
        return vanillaBurnTime;
    }
}
