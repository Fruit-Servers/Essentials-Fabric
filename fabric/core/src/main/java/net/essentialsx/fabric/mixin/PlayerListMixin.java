package net.essentialsx.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.JailListener;
import net.essentialsx.fabric.listener.PlayerListener;
import net.essentialsx.fabric.user.LazyLocation;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Join message capture (section 13.1), login gates (13.3) and respawn redirect (6.3 / spawn module).
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$captureJoinMessage(final PlayerList list, final Component message, final boolean overlay, final Connection connection, final ServerPlayer player, final CommonListenerCookie cookie) {
        final PlayerListener listener = EssentialsFabric.players();
        if (listener == null) {
            list.broadcastSystemMessage(message, overlay);
            return;
        }
        final Component replacement = listener.captureJoinMessage(player, message);
        if (replacement != null) {
            list.broadcastSystemMessage(replacement, overlay);
        }
    }

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void essentials$loginGates(final SocketAddress address, final GameProfile profile, final CallbackInfoReturnable<Component> cir) {
        final PlayerListener listener = EssentialsFabric.players();
        if (listener == null || profile == null) {
            return;
        }
        final PlayerList self = (PlayerList) (Object) this;
        final String ip = address instanceof InetSocketAddress isa && isa.getAddress() != null ? isa.getAddress().getHostAddress() : null;
        if (self.getBans().isBanned(profile) || self.getIpBans().isBanned(address)) {
            final Component custom = listener.banMessage(profile, ip);
            if (custom != null) {
                cir.setReturnValue(custom);
            }
            return;
        }
        if (!self.isWhiteListed(profile)) {
            if (listener.canBypassWhitelist(profile.getId())) {
                // Whitelisted by permission: still honour the player limit below.
                if (self.getPlayerCount() >= self.getMaxPlayers() && !self.canBypassPlayerLimit(profile) && !listener.canBypassFullServer(profile.getId())) {
                    final Component full = listener.serverFullMessage();
                    cir.setReturnValue(full != null ? full : Component.translatable("multiplayer.disconnect.server_full"));
                    return;
                }
                cir.setReturnValue(null);
                return;
            }
            final Component custom = listener.whitelistMessage();
            if (custom != null) {
                cir.setReturnValue(custom);
            }
            return;
        }
        if (self.getPlayerCount() >= self.getMaxPlayers() && !self.canBypassPlayerLimit(profile)) {
            if (listener.canBypassFullServer(profile.getId())) {
                cir.setReturnValue(null);
                return;
            }
            final Component custom = listener.serverFullMessage();
            if (custom != null) {
                cir.setReturnValue(custom);
            }
        }
    }

    @Redirect(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition essentials$respawnLocation(final ServerPlayer player, final boolean alive, final DimensionTransition.PostDimensionTransition post, final ServerPlayer original, final boolean keepEverything, final Entity.RemovalReason reason) {
        final Essentials ess = EssentialsFabric.get();
        if (ess != null) {
            LazyLocation override = JailListener.respawnLocation(ess, player);
            if (override == null && ess.getRespawnResolver() != null) {
                override = ess.getRespawnResolver().resolve(player, !alive);
            }
            if (override != null) {
                final ServerLevel level = override.level(ess.getServer());
                if (level != null) {
                    return new DimensionTransition(level, new Vec3(override.x(), override.y(), override.z()), Vec3.ZERO, override.yaw(), override.pitch(), post);
                }
            }
        }
        return player.findRespawnPositionAndUseSpawnBlock(alive, post);
    }
}
