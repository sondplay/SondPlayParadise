package com.sondplay.paradise;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.Name("SondPlayParadise")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"com.sondplay.paradise.transform"})
@IFMLLoadingPlugin.SortingIndex(1001)
public class ParadisePlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.sondplay.paradise.transform.TransformerDispatcher"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // Config is loaded later in @Mod preInit
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
