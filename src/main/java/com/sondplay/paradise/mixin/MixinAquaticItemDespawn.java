package com.sondplay.paradise.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityItem.class, remap = false)
public abstract class MixinAquaticItemDespawn {

    @Shadow public int field_70292_b; // age

    @Inject(method = "func_70071_h_", at = @At("HEAD"))
    private void paradise$fastDespawnWaterDrops(CallbackInfo ci) {
        if (this.field_70292_b >= 5400) return;

        Entity self = (Entity)(Object)this;
        int ticks = self.field_70173_aa;

        if (ticks % 40 != 20) return;

        if (!self.func_70090_H()) return;

        EntityItem item = (EntityItem)(Object)this;
        String thrower = item.func_145800_j();
        if (thrower != null && thrower.length() > 0) return;

        net.minecraft.item.ItemStack stack = item.func_92059_d();
        if (stack == null) return;

        if (stack.func_77976_d() <= 1) return;
        if (stack.func_77948_v()) return;
        if (stack.func_82837_s()) return;

        net.minecraft.item.Item itemType = stack.func_77973_b();
        if (itemType != null) {
            String className = itemType.getClass().getName();
            if (className.startsWith("danger.orespawn.")) return;
        }

        this.field_70292_b = 5400;
    }
}
