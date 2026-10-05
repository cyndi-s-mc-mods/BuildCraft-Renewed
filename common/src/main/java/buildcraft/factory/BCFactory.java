package buildcraft.factory;

public final class BCFactory {
    private BCFactory() {}

    public static void init() {
        BCFactoryBlocks.init();
        BCFactoryItems.init();
        BCFactoryMenus.init();
    }

    public static void setup() {}
}
