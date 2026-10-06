package buildcraft.core;

public final class BCCore {
    private BCCore() {}

    public static void init() {
        BCCoreComponents.init();
        BCCoreBlocks.init();
        BCCoreItems.init();
        BCCoreMenus.init();
        BCCoreEntities.init();
        BCCoreWorldGen.init();
        buildcraft.core.statements.BCCoreStatements.init();
    }
}
