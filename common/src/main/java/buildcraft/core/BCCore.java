package buildcraft.core;

public final class BCCore {
    private BCCore() {}

    public static void init() {
        BCCoreBlocks.init();
        BCCoreItems.init();
    }
}
