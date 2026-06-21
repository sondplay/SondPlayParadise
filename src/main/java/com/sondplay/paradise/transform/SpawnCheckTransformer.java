package com.sondplay.paradise.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Injects spawn check caching into OreSpawn entity classes.
 * Wraps getCanSpawnHere (func_70601_bi) with a cache lookup/store
 * to avoid expensive biome/light/collision checks every tick.
 *
 * Ported from OreSpawnSpawnCheckTransformer.java
 */
public class SpawnCheckTransformer {

    private static final String CACHE_CLASS = "com/sondplay/paradise/handler/SpawnCheckCache";
    private static final Set<String> BOSS_CLASSES = new HashSet<>();

    static {
        BOSS_CLASSES.add("danger.orespawn.Godzilla");
        BOSS_CLASSES.add("danger.orespawn.TheKing");
        BOSS_CLASSES.add("danger.orespawn.TheQueen");
        BOSS_CLASSES.add("danger.orespawn.Mobzilla");
        BOSS_CLASSES.add("danger.orespawn.Kraken");
        BOSS_CLASSES.add("danger.orespawn.Mothra");
        BOSS_CLASSES.add("danger.orespawn.Kyuubi");
    }

    public byte[] transform(String className, byte[] bytes) {
        if (!className.startsWith("danger.orespawn.")) return bytes;
        if (BOSS_CLASSES.contains(className)) return bytes;

        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        boolean patched = false;
        for (Object obj : cn.methods) {
            MethodNode mn = (MethodNode) obj;
            // func_70601_bi = getCanSpawnHere
            if (mn.name.equals("func_70601_bi") && mn.desc.equals("()Z")) {
                injectCache(cn.name, mn);
                patched = true;
                break;
            }
        }

        if (patched) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            System.out.println("[Paradise] SpawnCheck: Cached getCanSpawnHere in " + className);
            return cw.toByteArray();
        }
        return bytes;
    }

    private void injectCache(String ownerClass, MethodNode mn) {
        InsnList inject = new InsnList();

        // if (SpawnCheckCache.has(this)) return SpawnCheckCache.get(this);
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CACHE_CLASS,
            "has", "(Ljava/lang/Object;)Z", false));
        LabelNode skipCache = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IFEQ, skipCache));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CACHE_CLASS,
            "get", "(Ljava/lang/Object;)Z", false));
        inject.add(new InsnNode(Opcodes.IRETURN));
        inject.add(skipCache);

        mn.instructions.insert(inject);

        // Before each IRETURN, store result in cache
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.IRETURN && insn != inject.getLast()) {
                InsnList store = new InsnList();
                store.add(new InsnNode(Opcodes.DUP));
                store.add(new VarInsnNode(Opcodes.ALOAD, 0));
                store.add(new InsnNode(Opcodes.SWAP));
                store.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CACHE_CLASS,
                    "put", "(Ljava/lang/Object;Z)V", false));
                mn.instructions.insertBefore(insn, store);
            }
        }
    }
}
