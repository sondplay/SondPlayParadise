package com.sondplay.paradise.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Fixes cascading worldgen by adding +8 to chunk*16 coordinate calculations.
 * Detects [iload][bipush 16][imul] and inserts [bipush 8][iadd] after.
 * Also detects [iload][iconst_4][ishl] (Forestry pattern).
 *
 * Ported from PatchWorldgenOffset.java standalone tool.
 */
public class WorldgenOffsetTransformer {

    private static final Set<String> TARGETS = new HashSet<>();

    static {
        TARGETS.add("com.tihyo.legends.worldgens");
        TARGETS.add("com.tihyo.superheroes.worldgens");
        TARGETS.add("net.nevermine.dimension.WorldGen");
        TARGETS.add("mysticstones.worldgen.WorldGeneratorStones");
        TARGETS.add("mysticores.worldgen.WorldGenOres");
        TARGETS.add("team.chisel.world.GeneratorChisel");
        TARGETS.add("ic2.core.IC2");
        TARGETS.add("powercrystals.minefactoryreloaded.world.MineFactoryReloadedWorldGen");
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
            if (mn.name.contains("generate") || mn.name.contains("Generate")) {
                totalPatched += patchMethod(mn);
            }
        }

        if (totalPatched > 0) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            System.out.println("[Paradise] WorldgenOffset: Patched " + totalPatched +
                " sites in " + className);
            return cw.toByteArray();
        }
        return bytes;
    }

    private int patchMethod(MethodNode mn) {
        int patched = 0;
        AbstractInsnNode[] insns = mn.instructions.toArray();

        for (int i = 0; i < insns.length - 2; i++) {
            // Pattern: iload, bipush 16, imul
            if (insns[i].getOpcode() >= Opcodes.ILOAD && insns[i].getOpcode() <= Opcodes.ALOAD &&
                insns[i + 1] instanceof IntInsnNode &&
                ((IntInsnNode) insns[i + 1]).operand == 16 &&
                insns[i + 1].getOpcode() == Opcodes.BIPUSH &&
                insns[i + 2].getOpcode() == Opcodes.IMUL) {

                // Insert bipush 8; iadd after the imul
                InsnList patch = new InsnList();
                patch.add(new IntInsnNode(Opcodes.BIPUSH, 8));
                patch.add(new InsnNode(Opcodes.IADD));
                mn.instructions.insert(insns[i + 2], patch);
                patched++;
                i += 4; // skip past inserted instructions
            }
            // Pattern: iload, iconst_4, ishl (Forestry)
            else if (insns[i].getOpcode() >= Opcodes.ILOAD && insns[i].getOpcode() <= Opcodes.ALOAD &&
                     insns[i + 1].getOpcode() == Opcodes.ICONST_4 &&
                     insns[i + 2].getOpcode() == Opcodes.ISHL) {

                InsnList patch = new InsnList();
                patch.add(new IntInsnNode(Opcodes.BIPUSH, 8));
                patch.add(new InsnNode(Opcodes.IADD));
                mn.instructions.insert(insns[i + 2], patch);
                patched++;
                i += 4;
            }
        }
        return patched;
    }
}
