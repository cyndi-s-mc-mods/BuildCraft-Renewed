package buildcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-independent entry point. Each loader calls {@link #init} while the mod is constructed. */
public final class BuildCraft {
    public static final String MOD_ID = "buildcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("BuildCraft");

    private BuildCraft() {}

    public static void init() {
        LOGGER.info("BuildCraft loading");
    }
}
