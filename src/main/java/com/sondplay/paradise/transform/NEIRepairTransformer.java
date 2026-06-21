package com.sondplay.paradise.transform;

import org.objectweb.asm.*;

/**
 * Neutralizes NEI's RepairRecipeHandler.buildCache() which performs an
 * O(n^2) scan over ItemList.items (~15k items x ~15k items), calling
 * ItemStack.copy() for every repairable permutation. This allocates ~23 million
 * objects and freezes the game.
 *
 * Replaces the method body with: cachedRecipes = new ArrayList(); return;
 *
 * Ported from PatchNEIRepair.java standalone tool.
 */
public class NEIRepairTransformer {

    private static final String CLASS = "codechicken/nei/recipe/RepairRecipeHandler";
    private static final String FIELD = "cachedRecipes";
    private static final String FIELD_DESC = "Ljava/util/List;";

    public byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(0);

        final boolean[] patched = {false};

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String sig, String[] ex) {
                final MethodVisitor target = super.visitMethod(access, name, desc, sig, ex);
                if (name.equals("buildCache") && desc.equals("()V")) {
                    patched[0] = true;
                    return new MethodVisitor(Opcodes.ASM5, null) {
                        @Override
                        public void visitCode() {
                            target.visitCode();
                            // cachedRecipes = new ArrayList();
                            target.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
                            target.visitInsn(Opcodes.DUP);
                            target.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList",
                                    "<init>", "()V", false);
                            target.visitFieldInsn(Opcodes.PUTSTATIC, CLASS, FIELD, FIELD_DESC);
                            // return;
                            target.visitInsn(Opcodes.RETURN);
                            target.visitMaxs(2, 1);
                            target.visitEnd();
                        }
                        @Override public void visitMaxs(int a, int b) {}
                        @Override public void visitEnd() {}
                        @Override public void visitInsn(int o) {}
                        @Override public void visitVarInsn(int o, int v) {}
                        @Override public void visitFieldInsn(int o, String w, String n, String d) {}
                        @Override public void visitMethodInsn(int o, String w, String n, String d, boolean i) {}
                        @Override public void visitTypeInsn(int o, String t) {}
                        @Override public void visitJumpInsn(int o, Label l) {}
                        @Override public void visitLabel(Label l) {}
                        @Override public void visitIntInsn(int o, int v) {}
                        @Override public void visitLdcInsn(Object c) {}
                        @Override public void visitIincInsn(int v, int i) {}
                        @Override public void visitFrame(int t, int n, Object[] l, int s, Object[] k) {}
                        @Override public void visitLineNumber(int l, Label s) {}
                        @Override public void visitLocalVariable(String n, String d, String s, Label a, Label e, int i) {}
                        @Override public void visitTryCatchBlock(Label s, Label e, Label h, String t) {}
                    };
                }
                return target;
            }
        }, 0);

        if (patched[0]) {
            System.out.println("[Paradise] NEI RepairRecipeHandler.buildCache() neutralized.");
        }
        return cw.toByteArray();
    }
}
