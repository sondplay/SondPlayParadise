package com.sondplay.paradise.mixin;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.ASMEventHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.ListenerList;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filters SuperHeroes (tihyo) LivingUpdateEvent listeners out of EventBus.post()
 * for non-player entities — at the source, so the dispatch loop never iterates
 * over the ~250 hero/villain handlers for mobs.
 *
 * This is the heavy win: instead of cancelling 250 invoke() calls per mob,
 * the dispatch array simply doesn't contain them for mobs.
 *
 * Safe: only LivingUpdateEvent is touched, only the tihyo/superheroes listeners
 * are removed, and only when the entity is a non-player. All other mods' listeners
 * (Forge, PowerGems, TF, AoA, etc) are kept untouched.
 */
@Mixin(value = cpw.mods.fml.common.eventhandler.EventBus.class, remap = false)
public abstract class MixinEventBus {

    private static final Logger LOGGER = LogManager.getLogger("SusPatch");

    // Cache: original listener array -> filtered array (without SHS handlers)
    // Keyed by identity of the original array, which ListenerList reuses/caches.
    private static final Map<IEventListener[], IEventListener[]> FILTERED_CACHE =
            new IdentityHashMap<>(16);

    // tihyo mod IDs confirmed from FML log
    private static final Set<String> SHS_MOD_IDS = new HashSet<String>() {{
        add("sus"); add("susaddon"); add("ironman"); add("legends");
    }};

    private static volatile Field ownerField = null;
    private static volatile Field readableField = null;
    private static volatile boolean fieldsResolved = false;

    private static volatile Field entityLivingField = null;
    private static volatile boolean entityFieldResolved = false;

    /**
     * Redirect the getListeners() call inside post(). For a non-player
     * LivingUpdateEvent, return a filtered array without SHS handlers.
     */
    @Redirect(
        method = "post",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/ListenerList;getListeners(I)[Lcpw/mods/fml/common/eventhandler/IEventListener;"
        )
    )
    private IEventListener[] paradise$filterListeners(ListenerList list, int id, Event event) {
        IEventListener[] original = list.getListeners(id);

        // Only touch LivingUpdateEvent
        if (!(event instanceof LivingEvent.LivingUpdateEvent)) return original;

        // Only for non-player entities (players keep full handler set)
        Object entity = getEntity((LivingEvent.LivingUpdateEvent) event);
        if (entity == null) return original;
        if (entity.getClass().getName().contains("EntityPlayer")) return original;

        // Return cached filtered array if we've seen this exact array before
        IEventListener[] cached = FILTERED_CACHE.get(original);
        if (cached != null) return cached;

        IEventListener[] filtered = buildFiltered(original);
        FILTERED_CACHE.put(original, filtered);
        return filtered;
    }

    private static IEventListener[] buildFiltered(IEventListener[] original) {
        List<IEventListener> keep = new ArrayList<>(original.length);
        int removed = 0;
        for (IEventListener l : original) {
            if (isShsListener(l)) { removed++; continue; }
            keep.add(l);
        }
        if (removed == 0) return original; // nothing to filter
        LOGGER.info("[SusPatch] Filtered " + removed + " SHS listeners from mob LivingUpdate dispatch");
        return keep.toArray(new IEventListener[0]);
    }

    private static boolean isShsListener(IEventListener l) {
        if (!(l instanceof ASMEventHandler)) return false;
        if (!fieldsResolved) resolveFields();
        ASMEventHandler h = (ASMEventHandler) l;
        // Primary: ModContainer modId
        if (ownerField != null) {
            try {
                ModContainer owner = (ModContainer) ownerField.get(h);
                if (owner != null && SHS_MOD_IDS.contains(owner.getModId())) return true;
            } catch (Exception ignored) {}
        }
        // Fallback: readable string
        if (readableField != null) {
            try {
                String readable = (String) readableField.get(h);
                if (readable != null && (readable.contains("tihyo") || readable.contains("superheroes") || readable.contains("SuperHero"))) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static synchronized void resolveFields() {
        if (fieldsResolved) return;
        try {
            for (Field f : ASMEventHandler.class.getDeclaredFields()) {
                if (f.getType() == ModContainer.class) {
                    f.setAccessible(true);
                    ownerField = f;
                } else if (f.getType() == String.class && readableField == null) {
                    f.setAccessible(true);
                    readableField = f;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[SusPatch] field resolution failed: " + e);
        }
        fieldsResolved = true;
    }

    private static Object getEntity(LivingEvent.LivingUpdateEvent event) {
        if (!entityFieldResolved) resolveEntityField();
        if (entityLivingField == null) return null;
        try {
            return entityLivingField.get(event);
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized void resolveEntityField() {
        if (entityFieldResolved) return;
        try {
            for (Field f : LivingEvent.class.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    entityLivingField = f;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[SusPatch] EventBus entity field failed: " + e);
        }
        entityFieldResolved = true;
    }
}
