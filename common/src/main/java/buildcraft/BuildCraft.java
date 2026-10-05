package buildcraft;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import buildcraft.builders.BCBuilders;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.core.BCCore;
import buildcraft.energy.BCEnergy;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.factory.BCFactory;
import buildcraft.factory.BCFactoryConfig;
import buildcraft.lib.BCLib;
import buildcraft.lib.config.BCConfig;
import buildcraft.lib.platform.Platform;
import buildcraft.transport.BCTransport;
import buildcraft.transport.BCTransportConfig;

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
        Map<String, Class<?>> config = new LinkedHashMap<>();
        config.put("energy", BCEnergyConfig.class);
        config.put("factory", BCFactoryConfig.class);
        config.put("builders", BCBuildersConfig.class);
        config.put("transport", BCTransportConfig.class);
        BCConfig.load(Platform.INSTANCE.configDir().resolve("buildcraft.properties"), config);
        BCLib.init();
        BCCore.init();
        BCEnergy.init();
        BCTransport.init();
        BCFactory.init();
        BCBuilders.init();
    }

    private static boolean setUp = false;

    /** Called by each loader once registration has finished. Sets up things that need registered objects. */
    public static void setup() {
        if (setUp) return;
        setUp = true;
        BCEnergy.setup();
        BCTransport.setup();
        BCFactory.setup();
        BCBuilders.setup();
    }
}
