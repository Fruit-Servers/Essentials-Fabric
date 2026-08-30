package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.BlockListener;
import net.essentialsx.fabric.listener.PlayerListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quit message policy (13.1) and left-click-air detection for powertools / fly-click-jump (11.x).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Redirect(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$quitMessage(final PlayerList list, final Component message, final boolean overlay) {
        final PlayerListener listener = EssentialsFabric.players();
        final Component replacement = listener == null ? message : listener.quitMessage(this.player, message);
        if (replacement != null) {
            list.broadcastSystemMessage(replacement, overlay);
        }
    }

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void essentials$leftClickAir(final ServerboundSwingPacket packet, final CallbackInfo ci) {
        final BlockListener listener = EssentialsFabric.blocks();
        if (listener == null || this.player == null || !this.player.getServer().isSameThread()) {
            return;
        }
        // Only a "swing at nothing" is a left-click-air; attacks and block hits arrive through their own packets.
        final double reach = Math.max(this.player.blockInteractionRange(), this.player.entityInteractionRange());
        final Vec3 eye = this.player.getEyePosition();
        final Vec3 end = eye.add(this.player.getLookAngle().scale(reach));
        final HitResult block = this.player.pick(reach, 1.0F, false);
        if (block instanceof BlockHitResult && block.getType() != HitResult.Type.MISS) {
            return;
        }
        final AABB box = this.player.getBoundingBox().expandTowards(this.player.getLookAngle().scale(reach)).inflate(1.0D);
        final EntityHitResult entity = ProjectileUtil.getEntityHitResult(this.player, eye, end, box, e -> !e.isSpectator() && e.isPickable(), reach * reach);
        if (entity != null) {
            return;
        }
        listener.onLeftClickAir(this.player);
    }
}
