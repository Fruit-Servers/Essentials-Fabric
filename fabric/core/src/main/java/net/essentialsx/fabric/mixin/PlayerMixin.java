package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Display names (6.4) and the keep-inventory death pipeline (10.x).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void essentials$displayName(final CallbackInfoReturnable<Component> cir) {
        if (!((Object) this instanceof ServerPlayer)) {
            return;
        }
        final Essentials ess = EssentialsFabric.get();
        if (ess == null || ess.getDisplayNames() == null) {
            return;
        }
        final Component override = ess.getDisplayNames().getDisplayName(((Player) (Object) this).getUUID());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void essentials$keepInventory(final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && (Object) this instanceof ServerPlayer player && listener.shouldKeepInventory(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyVanishingCursedItems", at = @At("HEAD"), cancellable = true)
    private void essentials$keepVanishing(final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && (Object) this instanceof ServerPlayer player && listener.shouldKeepInventory(player)) {
            ci.cancel();
        }
    }
}
