package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanished players are never targeted by mobs. */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void essentials$target(final LivingEntity target, final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && target instanceof ServerPlayer player && !listener.canBeTargeted(player)) {
            ci.cancel();
        }
    }
}
