package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.EntityListener;
import net.essentialsx.fabric.listener.JailListener;
import net.essentialsx.fabric.listener.PlayerListener;
import net.essentialsx.fabric.user.LazyLocation;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Death hook (13.4), chat ignore recipients (13.x), tab-list names (6.4), jail teleport override + game-mode lock (7.x).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique
    private boolean essentials$teleportOverrideActive;

    /** Death bookkeeping (keep-inv/keep-xp flags, back-on-death) before vanilla drops loot and XP below. */
    @Inject(method = "die", at = @At("HEAD"))
    private void essentials$onDeath(final DamageSource source, final CallbackInfo ci) {
        final EntityListener listener = EssentialsFabric.entities();
        if (listener != null) {
            listener.onDeath((ServerPlayer) (Object) this);
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void essentials$filterChat(final OutgoingChatMessage message, final boolean filtered, final ChatType.Bound bound, final CallbackInfo ci) {
        final PlayerListener listener = EssentialsFabric.players();
        if (listener == null || !(message instanceof OutgoingChatMessage.Player playerMessage)) {
            return;
        }
        if (!listener.shouldReceiveChat((ServerPlayer) (Object) this, playerMessage.message().sender())) {
            ci.cancel();
        }
    }

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void essentials$tabListName(final CallbackInfoReturnable<Component> cir) {
        final Essentials ess = EssentialsFabric.get();
        if (ess == null || ess.getDisplayNames() == null) {
            return;
        }
        final Component override = ess.getDisplayNames().getTabListName(((ServerPlayer) (Object) this).getUUID());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void essentials$gameModeLock(final GameType type, final CallbackInfoReturnable<Boolean> cir) {
        final Essentials ess = EssentialsFabric.get();
        if (ess != null && !JailListener.canChangeGameMode(ess, (ServerPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void essentials$jailTeleport(final ServerLevel level, final double x, final double y, final double z, final float yaw, final float pitch, final CallbackInfo ci) {
        if (essentials$redirectToJail(ci)) {
            ci.cancel();
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), cancellable = true)
    private void essentials$jailTeleportRelative(final ServerLevel level, final double x, final double y, final double z, final Set<RelativeMovement> relative, final float yaw, final float pitch, final CallbackInfoReturnable<Boolean> cir) {
        if (essentials$redirectToJail(null)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean essentials$redirectToJail(final CallbackInfo ci) {
        if (essentials$teleportOverrideActive) {
            return false;
        }
        final Essentials ess = EssentialsFabric.get();
        if (ess == null) {
            return false;
        }
        final ServerPlayer self = (ServerPlayer) (Object) this;
        final LazyLocation jail = JailListener.overrideTeleport(ess, self);
        if (jail == null) {
            return false;
        }
        final ServerLevel jailLevel = jail.level(ess.getServer());
        if (jailLevel == null) {
            return false;
        }
        essentials$teleportOverrideActive = true;
        try {
            self.teleportTo(jailLevel, jail.x(), jail.y(), jail.z(), jail.yaw(), jail.pitch());
        } finally {
            essentials$teleportOverrideActive = false;
        }
        return true;
    }
}
