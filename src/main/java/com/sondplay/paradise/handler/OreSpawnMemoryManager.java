package com.sondplay.paradise.handler;

import com.sondplay.paradise.ParadiseConfig;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OreSpawnMemoryManager {
    private static final Logger LOGGER = LogManager.getLogger("OreSpawnMemMgr");
    private static final double HEAP_THRESHOLD = 0.75;
    private static final AtomicLong culledEntities = new AtomicLong(0);
    private static long lastLogTime = 0;
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Object world;
        try {
            java.lang.reflect.Field worldField = event.getClass().getField("world");
            world = worldField.get(event);
            boolean isRemote = (boolean) world.getClass().getField("isRemote").get(world);
            if (isRemote) return;
        } catch (Exception e) { return; }

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        try {
            int softMobCap = ParadiseConfig.mobCapSoft;
            int hardMobCap = ParadiseConfig.mobCapHard;

            Runtime runtime = Runtime.getRuntime();
            long maxMem = runtime.maxMemory();
            long usedMem = runtime.totalMemory() - runtime.freeMemory();
            double heapUsage = (double) usedMem / maxMem;

            Object worldBorder = world.getClass().getMethod("getWorldBorder").invoke(world);
            Object aabb = worldBorder.getClass().getMethod("func_151116_d").invoke(worldBorder);
            Class<?> entityMobClass = Class.forName("net.minecraft.entity.monster.EntityMob");
            List<?> mobs = (List<?>) world.getClass()
                .getMethod("getEntitiesWithinAABB", Class.class, Object.class)
                .invoke(world, entityMobClass, aabb);

            int mobCount = mobs.size();
            long now = System.currentTimeMillis();
            boolean shouldLog = (now - lastLogTime) > 10000;

            if (mobCount > hardMobCap || heapUsage > HEAP_THRESHOLD) {
                int toRemove = 0;
                if (heapUsage > HEAP_THRESHOLD) {
                    toRemove = Math.max(20, (int) (mobCount * 0.15));
                } else {
                    toRemove = mobCount - softMobCap;
                }

                int removed = 0;
                for (Object mob : mobs) {
                    if (removed >= toRemove) break;
                    String mobClass = mob.getClass().getName();
                    if (mobClass.startsWith("danger.orespawn.")) {
                        mob.getClass().getMethod("setDead").invoke(mob);
                        culledEntities.incrementAndGet();
                        removed++;
                    }
                }

                if (shouldLog) {
                    LOGGER.info(String.format(
                        "[OreSpawnMemMgr] Mobs: %d (cap: %d) | Heap: %.1f%% | Culled: %d | Total: %d",
                        mobCount, hardMobCap, heapUsage * 100, removed, culledEntities.get()
                    ));
                    lastLogTime = now;
                }
            } else if (shouldLog) {
                LOGGER.info(String.format(
                    "[OreSpawnMemMgr] OK — Mobs: %d | Heap: %.1f%%",
                    mobCount, heapUsage * 100
                ));
                lastLogTime = now;
            }
        } catch (Exception e) {
            LOGGER.warn("[OreSpawnMemMgr] Error: " + e.getMessage());
        }
    }
}
