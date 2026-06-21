package com.sondplay.paradise.mixin;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.ASMEventHandler;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = ASMEventHandler.class, remap = false)
public abstract class MixinEventHandler {

    private static final Logger LOGGER = LogManager.getLogger("SusPatch");

    // Cache per handler instance — computed once, never changes
    private static final Map<ASMEventHandler, Boolean> SHS_HANDLER_CACHE =
            new IdentityHashMap<>(512);

    // Per-player suit cache: entityId -> [lastCheckMs, hasSuit(1/0)]
    // Checked at most once per second (1000ms) instead of every tick
    private static final Map<Integer, long[]> PLAYER_SUIT_CACHE =
            new ConcurrentHashMap<>(8);
    private static final long PLAYER_CHECK_MS = 1000L;

    // Cleanup counter — avoids unbounded map growth on player disconnect
    private static long invokeCount = 0L;

    // tihyo mod IDs confirmed from FML log: sus, susaddon, ironman, legends
    private static final Set<String> SHS_MOD_IDS = new HashSet<String>() {{
        add("sus"); add("susaddon"); add("ironman"); add("legends");
    }};

    private static volatile Field ownerField = null;
    private static volatile Field readableField = null;
    private static volatile boolean fieldResolved = false;

    private static volatile Field entityLivingField = null;
    private static volatile boolean entityFieldResolved = false;

    private static volatile Field armorInventoryField = null;
    private static volatile Field mainInventoryField = null;
    private static volatile Field inventoryField = null;
    private static volatile Method getEntityIdMethod = null;
    private static volatile Method getItemMethod = null;
    private static volatile boolean playerFieldsResolved = false;

    @Inject(method = "invoke", at = @At("HEAD"), cancellable = true)
    private void paradise$skipSusForNonPlayer(Event event, CallbackInfo ci) {
        if (!(event instanceof LivingEvent.LivingUpdateEvent)) return;

        ASMEventHandler self = (ASMEventHandler) (Object) this;

        Boolean isShsHandler = SHS_HANDLER_CACHE.get(self);
        if (isShsHandler == null) {
            isShsHandler = computeIsShsHandler(self);
            SHS_HANDLER_CACHE.put(self, isShsHandler);
        }
        if (!isShsHandler) return;

        Object entity = getEntityFromEvent((LivingEvent.LivingUpdateEvent) event);
        if (entity == null) { ci.cancel(); return; }

        if (!entity.getClass().getName().contains("EntityPlayer")) {
            ci.cancel();
            return;
        }

        // Periodic cleanup every ~8192 invocations
        if ((++invokeCount & 0x1FFFL) == 0L) {
            PLAYER_SUIT_CACHE.clear();
        }

        int entityId = getEntityId(entity);
        long now = System.currentTimeMillis();
        long[] entry = PLAYER_SUIT_CACHE.get(entityId);
        if (entry == null || (now - entry[0]) > PLAYER_CHECK_MS) {
            boolean hasSuit = playerHasShsItem(entity);
            entry = new long[]{now, hasSuit ? 1L : 0L};
            PLAYER_SUIT_CACHE.put(entityId, entry);
        }
        if (entry[1] == 0L) {
            ci.cancel();
        }
    }

    private static boolean computeIsShsHandler(ASMEventHandler handler) {
        if (!fieldResolved) resolveFields();
        // Primary: check by ModContainer modId (fast, authoritative)
        if (ownerField != null) {
            try {
                ModContainer owner = (ModContainer) ownerField.get(handler);
                if (owner != null && SHS_MOD_IDS.contains(owner.getModId())) return true;
            } catch (Exception ignored) {}
        }
        // Fallback: check readable string if owner lookup failed
        if (readableField != null) {
            try {
                String readable = (String) readableField.get(handler);
                if (readable != null && (readable.contains("tihyo") || readable.contains("superheroes") || readable.contains("SuperHero"))) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static synchronized void resolveFields() {
        if (fieldResolved) return;
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
            LOGGER.info("[SusPatch] fields resolved: owner=" + (ownerField != null) + " readable=" + (readableField != null));
        } catch (Exception e) {
            LOGGER.warn("[SusPatch] field resolution failed: " + e);
        }
        fieldResolved = true;
    }

    private static Object getEntityFromEvent(LivingEvent.LivingUpdateEvent event) {
        if (!entityFieldResolved) resolveEntityLivingField();
        if (entityLivingField == null) return null;
        try {
            return entityLivingField.get(event);
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized void resolveEntityLivingField() {
        if (entityFieldResolved) return;
        try {
            for (Field f : LivingEvent.class.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    entityLivingField = f;
                    LOGGER.info("[SusPatch v2] entityLiving field: " + f.getName());
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[SusPatch v2] entityLiving field failed: " + e);
        }
        entityFieldResolved = true;
    }

    private static int getEntityId(Object entity) {
        try {
            if (getEntityIdMethod == null) {
                try {
                    getEntityIdMethod = entity.getClass().getMethod("getEntityId");
                } catch (NoSuchMethodException ex) {
                    getEntityIdMethod = entity.getClass().getMethod("func_145782_y");
                }
                getEntityIdMethod.setAccessible(true);
            }
            return (Integer) getEntityIdMethod.invoke(entity);
        } catch (Exception e) {
            return System.identityHashCode(entity);
        }
    }

    private static synchronized void resolvePlayerFields(Object player) {
        if (playerFieldsResolved) return;
        try {
            Class<?> cls = player.getClass();
            while (cls != null) {
                for (Field f : cls.getDeclaredFields()) {
                    String typeName = f.getType().getSimpleName();
                    if (typeName.equals("InventoryPlayer") || typeName.equals("IInventory")) {
                        f.setAccessible(true);
                        inventoryField = f;
                        break;
                    }
                }
                if (inventoryField != null) break;
                cls = cls.getSuperclass();
            }
            if (inventoryField != null) {
                Object inv = inventoryField.get(player);
                int found = 0;
                for (Field f : inv.getClass().getFields()) {
                    if (f.getType().isArray() && f.getType().getComponentType().getSimpleName().equals("ItemStack")) {
                        f.setAccessible(true);
                        if (armorInventoryField == null) armorInventoryField = f;
                        else { mainInventoryField = f; found++; }
                        if (found > 0) break;
                    }
                }
                if (armorInventoryField != null) {
                    Object[] armor = (Object[]) armorInventoryField.get(inv);
                    for (Object stack : armor) {
                        if (stack != null) {
                            for (Method m : stack.getClass().getMethods()) {
                                if (m.getName().equals("getItem") || m.getName().equals("func_77973_b")) {
                                    m.setAccessible(true);
                                    getItemMethod = m;
                                    break;
                                }
                            }
                            if (getItemMethod != null) break;
                        }
                    }
                }
            }
            LOGGER.info("[SusPatch v2] player fields resolved");
        } catch (Exception e) {
            LOGGER.warn("[SusPatch v2] player field resolution failed: " + e);
        }
        playerFieldsResolved = true;
    }

    private static boolean playerHasShsItem(Object player) {
        try {
            if (!playerFieldsResolved) resolvePlayerFields(player);
            if (inventoryField == null || armorInventoryField == null) return true;
            Object inv = inventoryField.get(player);
            Object[] armor = (Object[]) armorInventoryField.get(inv);
            for (Object stack : armor) {
                if (stack != null && isShsStack(stack)) return true;
            }
            if (mainInventoryField != null) {
                Object[] main = (Object[]) mainInventoryField.get(inv);
                for (int i = 0; i < Math.min(9, main.length); i++) {
                    if (main[i] != null && isShsStack(main[i])) return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isShsStack(Object stack) {
        try {
            if (getItemMethod == null) return false;
            Object item = getItemMethod.invoke(stack);
            if (item == null) return false;
            String itemClass = item.getClass().getName();
            return itemClass.contains("superheroes") || itemClass.contains("tihyo");
        } catch (Exception e) {
            return false;
        }
    }
}
