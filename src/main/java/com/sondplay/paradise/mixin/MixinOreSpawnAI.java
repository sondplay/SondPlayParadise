package com.sondplay.paradise.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = EntityLiving.class, remap = false)
public abstract class MixinOreSpawnAI {

    private static final String ORESPAWN_PACKAGE = "danger.orespawn.";
    private static final double NEAR_DIST_SQ = 32.0 * 32.0;
    private static final double FAR_DIST_SQ = 64.0 * 64.0;
    private static final int NEAR_THROTTLE = 3;
    private static final int FAR_THROTTLE = 5;

    private static final Set<String> BOSS_CLASSES = new HashSet<String>() {{
        add("Godzilla"); add("TheKing"); add("TheQueen"); add("Vortex");
        add("Kraken"); add("Mothra"); add("Kyuubi"); add("Mobzilla");
        add("GiantRobot"); add("SpiderRobot");
        add("Nightmare"); add("ThePrince"); add("ThePrinceAdult"); add("ThePrinceTeen");
        add("ThePrincess"); add("Cephadrome"); add("Hammerhead"); add("HerculesBeetle");
    }};

    @Inject(method = "func_70619_bc", at = @At("HEAD"), cancellable = true)
    private void paradise$throttleOreSpawnAI(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        Class<?> cls = self.getClass();
        if (!cls.getName().startsWith(ORESPAWN_PACKAGE)) return;
        if (BOSS_CLASSES.contains(cls.getSimpleName())) return;

        World world = self.field_70170_p;
        int ticks = self.field_70173_aa;

        EntityPlayer nearest = world.func_72890_a(self, -1.0D);
        if (nearest == null) {
            if (ticks % FAR_THROTTLE != 0) ci.cancel();
            return;
        }

        double distSq = self.func_70068_e(nearest);

        if (distSq <= NEAR_DIST_SQ) {
            return;
        } else if (distSq <= FAR_DIST_SQ) {
            if (ticks % NEAR_THROTTLE != 0) ci.cancel();
        } else {
            if (ticks % FAR_THROTTLE != 0) ci.cancel();
        }
    }
}
