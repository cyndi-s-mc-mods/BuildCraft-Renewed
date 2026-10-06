package buildcraft.builders;

public final class BCBuilders {
    private BCBuilders() {}

    public static void init() {
        BCBuildersComponents.init();
        BCBuildersBlocks.init();
        BCBuildersItems.init();
        BCBuildersMenus.init();
        buildcraft.core.marker.VolumeBoxAddon.register(buildcraft.builders.addon.AddonFillerPlanner.TYPE,
            buildcraft.builders.addon.AddonFillerPlanner::new);
        BCBuildersStatements.init();
    }

    public static void setup() {}
}
