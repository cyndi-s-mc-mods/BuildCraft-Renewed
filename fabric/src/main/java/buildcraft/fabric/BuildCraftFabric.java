package buildcraft.fabric;

import buildcraft.BuildCraft;

import net.fabricmc.api.ModInitializer;

public class BuildCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BuildCraft.init();
    }
}
