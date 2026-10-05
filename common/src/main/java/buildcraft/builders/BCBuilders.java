package buildcraft.builders;

public final class BCBuilders {
    private BCBuilders() {}

    public static void init() {
        BCBuildersBlocks.init();
        BCBuildersMenus.init();
        BCBuildersStatements.init();
    }

    public static void setup() {}
}
