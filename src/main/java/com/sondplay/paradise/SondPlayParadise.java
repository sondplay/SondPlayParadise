package com.sondplay.paradise;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import com.sondplay.paradise.handler.OreSpawnMemoryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = SondPlayParadise.MODID, name = SondPlayParadise.NAME, version = SondPlayParadise.VERSION, acceptableRemoteVersions = "*")
public class SondPlayParadise {
    public static final String MODID = "sondplayparadise";
    public static final String NAME = "$ond'Play Paradise";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ParadiseConfig.init(event.getSuggestedConfigurationFile());
        LOGGER.info("[Paradise] Config loaded.");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (ParadiseConfig.enableMobCap) {
            MinecraftForge.EVENT_BUS.register(new OreSpawnMemoryManager());
            LOGGER.info("[Paradise] OreSpawn Memory Manager registered.");
        }
    }
}
