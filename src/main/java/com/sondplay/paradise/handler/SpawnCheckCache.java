package com.sondplay.paradise.handler;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpawnCheckCache {
    private static final Logger LOGGER = LogManager.getLogger("SpawnCheckCache");

    private static final Map<Object, CacheEntry> cache = new WeakHashMap<>();
    private static final long CACHE_TTL_MS = 100;

    private static long hits = 0;
    private static long misses = 0;
    private static long lastLogTime = 0;

    public static boolean has(Object entity) {
        CacheEntry entry = cache.get(entity);
        if (entry == null) return false;
        long now = System.currentTimeMillis();
        return (now - entry.timestamp) < CACHE_TTL_MS;
    }

    public static boolean get(Object entity) {
        CacheEntry entry = cache.get(entity);
        hits++;
        logIfNeeded(System.currentTimeMillis());
        return entry != null ? entry.result : false;
    }

    public static void put(Object entity, boolean result) {
        misses++;
        long now = System.currentTimeMillis();
        cache.put(entity, new CacheEntry(result, now));
        logIfNeeded(now);
    }

    private static void logIfNeeded(long now) {
        if (now - lastLogTime < 10000) return;
        long total = hits + misses;
        double hitRate = total > 0 ? (100.0 * hits / total) : 0;
        LOGGER.info(String.format(
            "[SpawnCheckCache] Hits: %d | Misses: %d | Hit rate: %.1f%% | Cache size: %d",
            hits, misses, hitRate, cache.size()
        ));
        hits = 0;
        misses = 0;
        lastLogTime = now;
    }

    private static class CacheEntry {
        final boolean result;
        final long timestamp;

        CacheEntry(boolean result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }
}
