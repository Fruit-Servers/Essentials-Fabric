package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends a chat line on behalf of a player (powertool {@code c:} entries, sudo chat).
 * The message is unsigned and goes through the normal chat broadcast (and therefore any
 * chat mods' cancellation hooks).
 */
public final class PlayerChat {
    private PlayerChat() {
    }

    public static void send(final Essentials ess, final ServerPlayer player, final String message) {
        if (player == null || message == null) {
            return;
        }
        final PlayerChatMessage chat = PlayerChatMessage.unsigned(player.getUUID(), message);
        ess.getServer().getPlayerList().broadcastChatMessage(chat, player, ChatType.bind(ChatType.CHAT, player));
    }
}
