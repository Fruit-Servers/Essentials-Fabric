package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanish: vanished players are never sent to observers that may not see them (section 8). */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void removePlayer(ServerPlayer player);

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void essentials$vanish(final ServerPlayer observer, final CallbackInfo ci) {
        final Essentials ess = EssentialsFabric.get();
        if (ess == null || ess.getVisibility() == null || !(this.entity instanceof ServerPlayer subject)) {
            return;
        }
        if (!ess.getVisibility().canSee(observer, subject)) {
            this.removePlayer(observer);
            ci.cancel();
        }
    }
}
