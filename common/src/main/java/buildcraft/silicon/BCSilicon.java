package buildcraft.silicon;

public final class BCSilicon {
    private BCSilicon() {}

    public static void init() {
        BCSiliconComponents.init();
        BCSiliconPlugs.init();
        BCSiliconBlocks.init();
        BCSiliconItems.init();
        BCSiliconMenus.init();
        buildcraft.silicon.statement.BCSiliconStatements.init();
    }

    public static void setup() {
        BCSiliconRecipes.init();
    }
}
