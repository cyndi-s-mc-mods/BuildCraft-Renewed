package buildcraft.builders;

public final class BCBuilders {
    private BCBuilders() {}

    public static void init() {
        BCBuildersComponents.init();
        BCBuildersBlocks.init();
        BCBuildersItems.init();
        BCBuildersMenus.init();
        BCBuildersStatements.init();
    }

    public static void setup() {}
}
