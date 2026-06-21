package com.sondplay.paradise;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class ParadiseConfig {
    // Note: In v1.0, ASM/Mixin patches are always active (they load before config).
    // These toggles are reserved for future use when early config loading is implemented.
    public static boolean enableWorldgenFix = true;
    public static boolean enableNEIFix = true;
    public static boolean enableOreSpawnThrottle = true;
    public static boolean enableItemDespawn = true;
    public static boolean enableEventFilter = true;
    public static boolean enableMorphFix = true;
    public static boolean enableMobCap = true;
    public static boolean enableSpawnCache = true;

    public static int mobCapSoft = 200;
    public static int mobCapHard = 300;

    public static void init(File configFile) {
        Configuration config = new Configuration(configFile);
        config.load();

        enableMobCap = config.getBoolean("enableMobCap", "general", true,
            "Enable OreSpawn mob population cap and heap pressure management");

        mobCapSoft = config.getInt("softCap", "mobcap", 200, 50, 1000,
            "Soft mob cap - culling starts above this");
        mobCapHard = config.getInt("hardCap", "mobcap", 300, 100, 2000,
            "Hard mob cap - immediate culling above this");

        if (config.hasChanged()) config.save();
    }
}
