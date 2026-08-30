package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep-XP death pipeline (10.x). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void essentials$keepXp(final Entity killer, final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && (Object) this instanceof ServerPlayer player && listener.shouldKeepXp(player)) {
            ci.cancel();
        }
    }
}
