package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Display-name and tab-list presentation (Section 12.3). The mixins on
 * {@code Player#getDisplayName} and {@code ServerPlayer#getTabListDisplayName} consult
 * this service so chat, join/quit messages and the tab list reflect nicknames.
 */
public class DisplayNames {
    private final Essentials ess;
    private final Map<UUID, Component> displayNames = new ConcurrentHashMap<>();
    private final Map<UUID, Component> listNames = new ConcurrentHashMap<>();

    public DisplayNames(final Essentials ess) {
        this.ess = ess;
    }

    /**
     * Recompute cached display/tab names for a user and push a tab-list update.
     */
    public void update(final User user) {
        final ServerPlayer player = user.getBase();
        if (player == null) {
            return;
        }
        final UUID uuid = player.getUUID();
        if (!ess.getSettings().changeDisplayName()) {
            displayNames.remove(uuid);
        } else {
            final String nick = user.getNick(true, true);
            if (nick.equals(user.getName())) {
                displayNames.remove(uuid);
            } else {
                displayNames.put(uuid, Text.get().legacy(nick));
            }
        }
        Component listName = null;
        if (user.isAfk() && ess.getSettings().isAfkListName()) {
            final String afkName = ess.getSettings().getAfkListName().replace("{PLAYER}", user.getDisplayName()).replace("{USERNAME}", user.getName());
            listName = Text.get().legacy(afkName);
        } else if (ess.getSettings().changePlayerListName() && ess.getSettings().changeDisplayName()) {
            final String name = user.getNick(ess.getSettings().isAddingPrefixInPlayerlist(), ess.getSettings().isAddingSuffixInPlayerlist());
            if (!name.equals(user.getName())) {
                listName = Text.get().legacy(name);
            }
        }
        if (listName == null) {
            listNames.remove(uuid);
        } else {
            listNames.put(uuid, listName);
        }
        // Push tab list update to everyone who can see this player
        final ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player));
        for (final ServerPlayer other : ess.getOnlinePlayers()) {
            if (ess.getVisibility().canSee(other, player)) {
                other.connection.send(packet);
            }
        }
    }

    public void remove(final UUID uuid) {
        displayNames.remove(uuid);
        listNames.remove(uuid);
    }

    /** Display name override for chat/join/quit or null for vanilla. */
    public Component getDisplayName(final UUID uuid) {
        return displayNames.get(uuid);
    }

    /** Tab-list override or null for vanilla. */
    public Component getTabListName(final UUID uuid) {
        return listNames.get(uuid);
    }
}
