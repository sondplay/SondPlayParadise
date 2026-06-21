package com.sondplay.paradise.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Eliminates per-feature position jitter that causes cascading worldgen.
 * Detects [bipush 16][invokevirtual Random.nextInt(I)I] and rewrites 16 to 1.
 * nextInt(1) always returns 0, anchoring features at chunk center.
 *
 * Only applies to specific generate methods (generateSurface, generateNether, etc.)
 * Ported from PatchCenterFeature.java standalone tool.
 */
public class WorldgenCenterTransformer {

    private static final Set<String> TARGETS = new HashSet<>();
    private static final Set<String> TARGET_METHODS = new HashSet<>();

    static {
        TARGETS.add("com.tihyo.legends.worldgens");
        TARGETS.add("com.tihyo.superheroes.worldgens");
        TARGETS.add("net.nevermine.dimension.WorldGen");
        TARGETS.add("mysticstones.worldgen.WorldGeneratorStones");
        TARGETS.add("mysticores.worldgen.WorldGenOres");
        TARGETS.add("team.chisel.world.GeneratorChisel");
        TARGETS.add("ic2.core.IC2");
        TARGETS.add("forestry.core.worldgen.WorldGenerator");
        TARGETS.add("extrabiomes.module.summa.worldgen.MountainRidgeGenerator");
        TARGETS.add("powercrystals.minefactoryreloaded.world.MineFactoryReloadedWorldGen");

        TARGET_METHODS.add("generateSurface");
        TARGET_METHODS.add("generateNether");
        TARGET_METHODS.add("generateEnd");
        TARGET_METHODS.add("genStandardOre");
        TARGET_METHODS.add("generateFeature");
        TARGET_METHODS.add("generateWorld");
        TARGET_METHODS.add("generateEmeraldOre");
        TARGET_METHODS.add("generate");
        TARGET_METHODS.add("generatePrecasia");
        TARGET_METHODS.add("generateAbyss");
        TARGET_METHODS.add("generateHaven");
        TARGET_METHODS.add("generateIromine");
        TARGET_METHODS.add("generateLunalus");
        TARGET_METHODS.add("generateGardencia");
        TARGET_METHODS.add("generateMysterium");
    }

    public static boolean isTarget(String className) {
        for (String target : TARGETS) {
            if (className.equals(target) || className.startsWith(target + ".") ||
                className.startsWith(target.replace('.', '/') + "/")) {
                return true;
            }
        }
        return false;
    }

    public byte[] transform(String className, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        int totalPatched = 0;
        for (Object obj : cn.methods) {
            MethodNode mn = (MethodNode) obj;
            if (isTargetMethod(mn.name)) {
                totalPatched += patchMethod(mn);
            }
        }

        if (totalPatched > 0) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            System.out.println("[Paradise] WorldgenCenter: Patched " + totalPatched +
                " nextInt(16)->nextInt(1) sites in " + className);
            return cw.toByteArray();
        }
        return bytes;
    }

    private boolean isTargetMethod(String methodName) {
        for (String target : TARGET_METHODS) {
            if (methodName.contains(target)) return true;
        }
        return false;
    }

    private int patchMethod(MethodNode mn) {
        int patched = 0;
        AbstractInsnNode[] insns = mn.instructions.toArray();

        for (int i = 0; i < insns.length - 1; i++) {
            // Pattern: bipush 16, invokevirtual java/util/Random.nextInt(I)I
            if (insns[i] instanceof IntInsnNode &&
                insns[i].getOpcode() == Opcodes.BIPUSH &&
                ((IntInsnNode) insns[i]).operand == 16 &&
                insns[i + 1] instanceof MethodInsnNode) {

                MethodInsnNode call = (MethodInsnNode) insns[i + 1];
                if (call.name.equals("nextInt") &&
                    call.owner.equals("java/util/Random") &&
                    call.desc.equals("(I)I")) {

                    ((IntInsnNode) insns[i]).operand = 1;
                    patched++;
                }
            }
        }
        return patched;
    }
}
