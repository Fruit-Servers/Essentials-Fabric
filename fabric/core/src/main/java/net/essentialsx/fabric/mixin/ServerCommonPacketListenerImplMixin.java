package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Vanish: strips vanished players from tab-list add packets sent to observers that may not see them. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void essentials$filterPlayerInfo(final Packet<?> packet, final PacketSendListener sendListener, final CallbackInfo ci) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || !info.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            return;
        }
        if (!((Object) this instanceof ServerGamePacketListenerImpl impl) || impl.player == null) {
            return;
        }
        final Essentials ess = EssentialsFabric.get();
        if (ess == null || ess.getVisibility() == null || ess.getVisibility().getVanished().isEmpty()) {
            return;
        }
        final ServerPlayer observer = impl.player;
        final List<ServerPlayer> visible = new ArrayList<>();
        boolean changed = false;
        for (final ClientboundPlayerInfoUpdatePacket.Entry entry : info.entries()) {
            final ServerPlayer subject = observer.getServer().getPlayerList().getPlayer(entry.profileId());
            if (subject == null) {
                return; // cannot rebuild safely; send as-is
            }
            if (ess.getVisibility().canSee(observer, subject)) {
                visible.add(subject);
            } else {
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        ci.cancel();
        if (!visible.isEmpty()) {
            ((ServerCommonPacketListenerImpl) (Object) this).send(new ClientboundPlayerInfoUpdatePacket(info.actions(), visible), sendListener);
        }
    }
}
