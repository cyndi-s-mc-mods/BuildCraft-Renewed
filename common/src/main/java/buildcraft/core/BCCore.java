package buildcraft.core;

public final class BCCore {
    private BCCore() {}

    public static void init() {
        BCCoreBlocks.init();
        BCCoreItems.init();
        BCCoreMenus.init();
        BCCoreWorldGen.init();
        buildcraft.core.statements.BCCoreStatements.init();
    }
}
