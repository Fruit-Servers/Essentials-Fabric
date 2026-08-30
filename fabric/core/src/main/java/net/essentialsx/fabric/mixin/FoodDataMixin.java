package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Natural regeneration is suppressed for frozen AFK players (upstream EntityRegainHealthEvent SATIATED). */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void essentials$naturalRegen(final Player player, final float amount) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null && player instanceof ServerPlayer serverPlayer && !listener.allowNaturalRegen(serverPlayer)) {
            return;
        }
        player.heal(amount);
    }
}
