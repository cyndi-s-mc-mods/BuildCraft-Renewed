package buildcraft.forge;

import buildcraft.BuildCraft;

import net.minecraftforge.fml.common.Mod;

@Mod(BuildCraft.MOD_ID)
public class BuildCraftForge {
    public BuildCraftForge() {
        BuildCraft.init();
    }
}
