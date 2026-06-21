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
public abstract class MixinOreSpawnOnUpdate {

    private static final double NEAR_DIST_SQ = 32.0 * 32.0;
    private static final int THROTTLE = 4;

    private static final Set<String> THROTTLE_CLASSES = new HashSet<String>() {{
        add("RockBase"); add("EntityAnt"); add("EntityRedAnt");
        add("WaterBall"); add("Coin");
        add("WormMedium"); add("WormSmall"); add("WormLarge");
        add("Cockateil"); add("Cassowary");
        add("EntityButterfly"); add("Dragonfly");
        add("EntityLunaMoth"); add("Firefly"); add("EntityMosquito");
        add("Bee"); add("Termite"); add("InkSack");
    }};

    @Inject(method = "func_70071_h_", at = @At("HEAD"), cancellable = true)
    private void paradise$throttleOreSpawnOnUpdate(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!THROTTLE_CLASSES.contains(self.getClass().getSimpleName())) return;

        int ticks = self.field_70173_aa;
        if (ticks % THROTTLE == 0) return;

        World world = self.field_70170_p;
        EntityPlayer nearest = world.func_72890_a(self, -1.0D);
        if (nearest != null && self.func_70068_e(nearest) <= NEAR_DIST_SQ) {
            return;
        }

        ci.cancel();
    }
}
