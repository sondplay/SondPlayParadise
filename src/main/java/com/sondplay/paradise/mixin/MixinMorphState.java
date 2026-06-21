package com.sondplay.paradise.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fixes Morph crash on entity death:
 *   ClassCastException: Object2ObjectOpenHashMap cannot be cast to HashMap
 *   at morph.common.morph.MorphState.parseTag
 *
 * Morph casts NBTTagCompound's internal map field to java.util.HashMap, but
 * Hodgepodge replaces that map with a fastutil Object2ObjectOpenHashMap.
 * Both implement java.util.Map, so we rewrite parseTag to use the Map interface.
 *
 * parseTag builds a sorted "{key:value,key:value}" string used as a unique morph
 * identifier. This overwrite preserves that exact format, just without the cast.
 *
 * NBTTagCompound is referenced as Object to avoid importing obfuscated MC classes;
 * the descriptor is fixed via the mixin refmap at apply time.
 */
@Mixin(targets = "morph.common.morph.MorphState", remap = false)
public abstract class MixinMorphState {

    /**
     * @author Paradise
     * @reason Avoid HashMap cast that crashes with fastutil-backed NBT maps (Hodgepodge)
     */
    @Overwrite
    public static String parseTag(net.minecraft.nbt.NBTTagCompound tag) {
        StringBuilder result = new StringBuilder();
        List<String> entries = new ArrayList<String>();

        Map<Object, Object> map = getTagMap(tag);
        if (map != null) {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                entries.add(e.getKey().toString() + ":" + map.get(e.getKey()));
            }
        }
        Collections.sort(entries);

        result.append("{");
        for (int i = 0; i < entries.size(); i++) {
            result.append(entries.get(i));
            if (i < entries.size() - 1) result.append(",");
        }
        result.append("}");
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> getTagMap(Object tag) {
        if (tag == null) return null;
        for (Field f : tag.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    return (Map<Object, Object>) f.get(tag);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
