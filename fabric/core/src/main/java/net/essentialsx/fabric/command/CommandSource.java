package net.essentialsx.fabric.command;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Wraps a Brigadier {@link CommandSourceStack}. Console, command blocks and RCON are
 * non-player sources; players resolve to an Essentials {@link User}.
 */
public class CommandSource {
    protected final Essentials ess;
    protected CommandSourceStack sender;

    public CommandSource(final Essentials ess, final CommandSourceStack base) {
        this.ess = ess;
        this.sender = base;
    }

    public final CommandSourceStack getSender() {
        return sender;
    }

    public final ServerPlayer getPlayer() {
        return sender.getPlayer();
    }

    public void sendTl(final String tlKey, final Object... args) {
        if (isPlayer()) {
            getUser().sendTl(tlKey, args);
            return;
        }
        final String translation = tlLiteral(tlKey, args);
        if (!translation.isEmpty()) {
            sendComponent(Text.get().deserializeMiniMessage(translation));
        }
    }

    public String tl(final String tlKey, final Object... args) {
        if (isPlayer()) {
            return getUser().playerTl(tlKey, args);
        }
        return tlLiteral(tlKey, args);
    }

    public Component tlComponent(final String tlKey, final Object... args) {
        if (isPlayer()) {
            return getUser().tlComponent(tlKey, args);
        }
        final String translation = tlLiteral(tlKey, args);
        return Text.get().deserializeMiniMessage(translation);
    }

    public void sendComponent(final Component component) {
        sendNative(Text.get().toNative(component));
    }

    public void sendNative(final net.minecraft.network.chat.Component component) {
        sender.sendSystemMessage(component);
    }

    public final User getUser() {
        final ServerPlayer player = getPlayer();
        if (player != null) {
            return ess.getUser(player);
        }
        return null;
    }

    public final boolean isPlayer() {
        return sender.getPlayer() != null;
    }

    public final boolean isConsole() {
        return !isPlayer() && sender.getEntity() == null;
    }

    public final CommandSourceStack setSender(final CommandSourceStack base) {
        return this.sender = base;
    }

    /**
     * Sends a legacy formatted (section sign) string.
     */
    public void sendMessage(final String message) {
        if (!message.isEmpty()) {
            sendNative(Text.get().legacy(message));
        }
    }

    public boolean isAuthorized(final String permission) {
        return !isPlayer() || getUser().isAuthorized(permission);
    }

    public String getSelfSelector() {
        return isPlayer() ? getPlayer().getGameProfile().getName() : "*";
    }

    public String getDisplayName() {
        return isPlayer() ? getUser().getDisplayName() : sender.getTextName();
    }

    public String getName() {
        return isPlayer() ? getPlayer().getGameProfile().getName() : sender.getTextName();
    }
}
