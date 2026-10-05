package buildcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import buildcraft.core.BCCore;
import buildcraft.energy.BCEnergy;
import buildcraft.lib.BCLib;
import buildcraft.lib.platform.Platform;

/** Loader-independent entry point. Each loader calls {@link #init} while the mod is constructed, before registration. */
public final class BuildCraft {
    public static final String MOD_ID = "buildcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("BuildCraft");

    private static boolean initialized = false;

    private BuildCraft() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("BuildCraft loading on {}", Platform.INSTANCE.loaderName());
        BCLib.init();
        BCCore.init();
        BCEnergy.init();
    }

    private static boolean setUp = false;

    /** Called by each loader once registration has finished. Sets up things that need registered objects. */
    public static void setup() {
        if (setUp) return;
        setUp = true;
        BCEnergy.setup();
    }
}
