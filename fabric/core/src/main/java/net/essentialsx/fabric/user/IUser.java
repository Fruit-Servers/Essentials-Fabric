package net.essentialsx.fabric.user;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.teleport.AsyncTeleport;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read/mutate contract for an Essentials user (mirrors upstream {@code net.ess3.api.IUser}).
 */
public interface IUser extends IMessageRecipient {
    boolean isAuthorized(String node);

    boolean isAuthorizedCached(String node);

    boolean isPermissionSet(String node);

    /** Online player backing this user, or null when offline. */
    ServerPlayer getBase();

    boolean isOnline();

    @Override
    UUID getUUID();

    @Override
    String getName();

    @Override
    String getDisplayName();

    String getFormattedNickname();

    String getNickname();

    LazyLocation getLocation();

    LazyLocation getLastLocation();

    LazyLocation getLogoutLocation();

    void setLastLocation();

    void setLastLocationIfChanged(LazyLocation origin);

    void setLogoutLocation();

    long getLastTeleportTimestamp();

    void setLastTeleportTimestamp(long time);

    long getLastHealTimestamp();

    void setLastHealTimestamp(long time);

    long getLastLogin();

    long getLastLogout();

    boolean isGodModeEnabled();

    boolean isAfk();

    boolean isJailed();

    String getJail();

    void setJail(String jail);

    boolean isMuted();

    boolean isHidden();

    boolean isVanished();

    boolean isTeleportEnabled();

    boolean isAutoTeleportEnabled();

    boolean isIgnoreExempt();

    boolean isIgnoredPlayer(IUser user);

    String getAfkMessage();

    void setAfkMessage(String message);

    long getAfkSince();

    boolean canAfford(BigDecimal cost);

    BigDecimal getMoney();

    void giveMoney(BigDecimal value) throws Exception;

    void takeMoney(BigDecimal value);

    CommandSource getSource();

    AsyncTeleport getAsyncTeleport();

    void enableInvulnerabilityAfterTeleport();

    void resetInvulnerabilityAfterTeleport();

    boolean hasInvulnerabilityAfterTeleport();

    boolean isIgnoreMsg();

    void setIgnoreMsg(boolean ignoreMsg);

    boolean isFreeze();

    void setFreeze(boolean freeze);

    List<MailMessage> getMailMessages();

    void setMailList(List<MailMessage> messages);

    void sendMail(IMessageRecipient sender, String message);

    void sendMail(IMessageRecipient sender, String message, long expireAt);

    String playerTl(String tlKey, Object... args);

    void sendTl(String tlKey, Object... args);

    void sendMessage(String message);

    net.kyori.adventure.text.Component tlComponent(String tlKey, Object... args);

    void sendComponent(net.kyori.adventure.text.Component component);

    void sendNative(net.minecraft.network.chat.Component component);
}
