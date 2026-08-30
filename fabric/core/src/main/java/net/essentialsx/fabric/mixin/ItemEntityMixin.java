package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item pickup policy for AFK / vanished players. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void essentials$pickup(final Player player, final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && player instanceof ServerPlayer serverPlayer && !listener.canPickup(serverPlayer)) {
            ci.cancel();
        }
    }
}
