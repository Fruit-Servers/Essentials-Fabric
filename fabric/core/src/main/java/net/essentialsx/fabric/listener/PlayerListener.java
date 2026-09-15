package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.TextInput;
import net.essentialsx.fabric.textreader.TextPager;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Player session behaviour (Section 13.3): join/quit flow, MOTD, mail notice, fly/speed
 * restoration, world change handling, chat mute/ignore and login gates.
 */
public class PlayerListener {
    private final Essentials ess;
    private final ConcurrentHashMap<UUID, Boolean> pendingMotd = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> joinMessages = new ConcurrentHashMap<>();

    public PlayerListener(final Essentials ess) {
        this.ess = ess;
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onQuit(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onRespawn(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onChangedWorld(player, origin, destination));
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> onChat(sender, message.signedContent()));
    }

    // ------------------------------------------------------------ join

    private void onJoin(final ServerPlayer player) {
        ess.getBackup().onPlayerJoin();
        final boolean existed = ess.getUsers().userExists(player.getUUID());
        final User user = ess.getUsers().loadOrCreate(player.getUUID(), player.getGameProfile().getName());
        user.update(player);
        ess.getUsers().getOnlineUserCache().put(player.getUUID(), user);
        ess.getUsers().trackName(player.getUUID(), player.getGameProfile().getName());
        if (ess.getEconomy() != null) {
            ess.getEconomy().preload(player.getUUID());
        }
        final long currentTime = System.currentTimeMillis();
        user.startTransaction();
        if (user.isNPC()) {
            user.setNPC(false);
        }
        user.checkMuteTimeout(currentTime);
        user.updateActivity(false, "JOIN");
        user.onSessionStart(ess.getServer().getTickCount());
        user.setLastKnownPosition(LazyLocation.of(player));
        user.stopTransaction();
        ess.getVisibility().onObserverJoin(player);
        final String vanillaMessage = joinMessages.remove(player.getUUID());
        joinFlow(user, currentTime, vanillaMessage, existed);
    }

    /**
     * Called by the PlayerList mixin instead of broadcasting the vanilla join message. Stores
     * the message; {@link #joinFlow} decides what to broadcast.
     */
    public Component captureJoinMessage(final ServerPlayer player, final Component vanilla) {
        joinMessages.put(player.getUUID(), Text.get().nativeToLegacy(vanilla));
        return null;
    }

    private boolean hideJoinQuitMessages() {
        return ess.getSettings().hasJoinQuitMessagePlayerCount() && ess.getOnlinePlayers().size() > ess.getSettings().getJoinQuitMessagePlayerCount();
    }

    private void joinFlow(final User user, final long currentTime, final String message, final boolean existed) {
        user.startTransaction();
        final String lastAccountName = user.getLastAccountName();
        user.setLastAccountName(user.getName());
        final boolean newUsername = lastAccountName != null && !lastAccountName.equals(user.getName());
        final boolean firstJoin = !existed || lastAccountName == null;
        if (ess.getSettings().isResetNickOnNameChange() && newUsername && user.getNickname() != null) {
            user.setNickname(null);
        }
        user.setLastLogin(currentTime, user.getBase().getIpAddress());
        user.setDisplayNick();
        updateCompass(user);
        user.setLeavingHidden(false);
        ess.getSleepManager().update(user);
        final String effectiveMessage;
        if (ess.getSettings().allowSilentJoinQuit() && (user.isAuthorized("essentials.silentjoin") || user.isAuthorized("essentials.silentjoin.vanish"))) {
            if (user.isAuthorized("essentials.silentjoin.vanish")) {
                user.setVanished(true);
            }
            effectiveMessage = null;
        } else if (message == null || hideJoinQuitMessages()) {
            effectiveMessage = null;
        } else if (ess.getSettings().isCustomJoinMessage()) {
            final String msg = (newUsername && ess.getSettings().isCustomNewUsernameMessage() ? ess.getSettings().getCustomNewUsernameMessage() : ess.getSettings().getCustomJoinMessage())
                .replace("{PLAYER}", user.getDisplayName()).replace("{USERNAME}", user.getName())
                .replace("{UNIQUE}", NumberFormat.getInstance().format(ess.getUsers().getUserCount()))
                .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size()))
                .replace("{UPTIME}", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()))
                .replace("{PREFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(user.getBase())))
                .replace("{SUFFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(user.getBase())))
                .replace("{OLDUSERNAME}", lastAccountName == null ? "" : lastAccountName);
            effectiveMessage = msg.isEmpty() ? null : msg;
        } else {
            effectiveMessage = message;
        }
        if (effectiveMessage != null && !effectiveMessage.isEmpty()) {
            ess.broadcastMessage(effectiveMessage);
        }
        ess.fireJoin(user, firstJoin);
        if (ess.getSettings().getMotdDelay() >= 0) {
            final int motdDelay = ess.getSettings().getMotdDelay() / 50;
            final Runnable motdTask = () -> motdFlow(user);
            if (motdDelay > 0) {
                pendingMotd.put(user.getUUID(), Boolean.TRUE);
                ess.scheduleSyncDelayedTask(() -> {
                    if (pendingMotd.remove(user.getUUID()) != null && user.isOnline()) {
                        motdTask.run();
                    }
                }, motdDelay);
            } else {
                motdTask.run();
            }
        }
        if (!ess.getSettings().isCommandDisabled("mail") && user.isAuthorized("essentials.mail")) {
            if (user.getUnreadMailAmount() == 0) {
                if (ess.getSettings().isNotifyNoNewMail()) {
                    user.sendTl("noNewMail");
                }
            } else {
                user.notifyOfMail();
            }
        }
        final ServerPlayer player = user.getBase();
        final boolean restoreFly = user.isFlyModeEnabled() && user.isAuthorized("essentials.fly");
        if (restoreFly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            if (ess.getSettings().isSendFlyEnableOnJoin()) {
                user.sendTl("flyMode", CommonPlaceholders.enableDisable(user.getSource(), true), user.getDisplayName());
            }
        }
        if (user.isAuthorized("essentials.fly.safelogin")) {
            player.fallDistance = 0;
            final boolean inWater = player.isInWater();
            if (!inWater && LocationUtil.shouldFly((ServerLevel) player.level(), user.getLocation())) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                if (!restoreFly && ess.getSettings().isSendFlyEnableOnJoin()) {
                    user.sendTl("flyMode", CommonPlaceholders.enableDisable(user.getSource(), true), user.getDisplayName());
                }
            }
        }
        if (!user.isAuthorized("essentials.speed")) {
            player.getAbilities().setFlyingSpeed(0.05f);
            player.getAbilities().setWalkingSpeed(0.1f);
            player.onUpdateAbilities();
        }
        if (user.isSocialSpyEnabled() && !user.isAuthorized("essentials.socialspy")) {
            user.setSocialSpyEnabled(false);
            ess.getLogger().info("Set socialspy to false for {} because they had it enabled without permission.", user.getName());
        }
        if (user.isFlyModeEnabled() && !user.isAuthorized("essentials.fly")) {
            user.setFlyModeEnabled(false);
            ess.getLogger().info("Set fly mode to false for {} because they had it enabled without permission.", user.getName());
        }
        if (user.isGodModeEnabled() && !user.isAuthorized("essentials.god")) {
            user.setGodModeEnabled(false);
            ess.getLogger().info("Set god mode to false for {} because they had it enabled without permission.", user.getName());
        }
        user.setConfirmingClearCommand(null);
        user.getConfirmingPayments().clear();
        user.stopTransaction();
        JailListener.onJoin(ess, user);
    }

    private void motdFlow(final User user) {
        IText tempInput = null;
        if (!ess.getSettings().isCommandDisabled("motd")) {
            try {
                tempInput = new TextInput(user.getSource(), "motd", true, ess);
            } catch (final IOException ex) {
                ess.getLogger().warn(ex.getMessage());
            }
        }
        final IText input = tempInput;
        if (input != null && !input.getLines().isEmpty() && user.isAuthorized("essentials.motd")) {
            final IText output = new KeywordReplacer(input, user.getSource(), ess);
            final TextPager pager = new TextPager(output, true);
            pager.showPage("1", null, "motd", user.getSource());
        }
    }

    public void updateCompass(final User user) {
        if (ess.getSettings().isCompassTowardsHomePerm() && !user.isAuthorized("essentials.home.compass")) {
            return;
        }
        final ServerPlayer player = user.getBase();
        if (player == null) {
            return;
        }
        final LazyLocation loc = user.getHome(user.getLocation());
        if (loc == null) {
            final BlockPos respawn = player.getRespawnPosition();
            if (respawn != null && player.getRespawnDimension() == player.level().dimension()) {
                player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(respawn, player.getRespawnAngle()));
            }
            return;
        }
        if (loc.sameWorld(LazyLocation.of(player))) {
            player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(loc.blockPos(), loc.yaw()));
        }
    }

    // ------------------------------------------------------------ quit

    private void onQuit(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null) {
            return;
        }
        pendingMotd.remove(user.getUUID());
        user.startTransaction();
        if (ess.getSettings().removeGodOnDisconnect() && user.isGodModeEnabled()) {
            user.setGodModeEnabled(false);
        }
        if (user.isVanished()) {
            user.setLeavingHidden(true);
            user.setVanished(false);
        }
        user.setLogoutLocation();
        user.getAsyncTeleport().cancel(false);
        user.clearTpaRequests();
        user.updateActivity(false, "QUIT");
        if (!user.isHidden()) {
            user.setLastLogout(System.currentTimeMillis());
        }
        user.onSessionEnd(ess.getServer().getTickCount());
        user.stopTransaction();
        ess.getDisplayNames().remove(player.getUUID());
        ess.getSleepManager().remove(player.getUUID());
        ess.getVisibility().remove(player.getUUID());
        user.setOffline();
        ess.getUsers().onQuit(player.getUUID());
        ess.getPlayerTimeWeather().remove(player.getUUID());
        user.dispose();
    }

    /**
     * Quit message policy (called from the mixin in place of the vanilla broadcast).
     */
    public Component quitMessage(final ServerPlayer player, final Component vanilla) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null) {
            return vanilla;
        }
        if (hideJoinQuitMessages() || ess.getSettings().allowSilentJoinQuit() && user.isAuthorized("essentials.silentquit")) {
            return null;
        }
        if (user.isLeavingHidden() || user.isVanished()) {
            return null;
        }
        if (ess.getSettings().isCustomQuitMessage()) {
            final String msg = ess.getSettings().getCustomQuitMessage()
                .replace("{PLAYER}", user.getDisplayName())
                .replace("{USERNAME}", user.getName())
                .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size() - 1))
                .replace("{UPTIME}", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()))
                .replace("{PREFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(player)))
                .replace("{SUFFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(player)));
            return msg.isEmpty() ? null : Text.get().legacy(msg);
        }
        return vanilla;
    }

    // ------------------------------------------------------------ respawn / world change

    private void onRespawn(final ServerPlayer player) {
        final User user = ess.getUser(player);
        user.update(player);
        updateCompass(user);
        user.setDisplayNick();
        user.setLastKnownPosition(LazyLocation.of(player));
        if (ess.getSettings().isTeleportInvulnerability()) {
            user.enableInvulnerabilityAfterTeleport();
        }
        if (user.isVanished()) {
            ess.getVisibility().resync(player);
        }
    }

    private void onChangedWorld(final ServerPlayer player, final ServerLevel origin, final ServerLevel destination) {
        ess.getPlayerTimeWeather().onChangeWorld(player);
        final User user = ess.getUser(player);
        user.update(player);
        user.setLastKnownPosition(LazyLocation.of(player));
        if (ess.getSettings().isWorldChangeFlyResetEnabled()) {
            if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE
                && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
                && !user.isAuthorized("essentials.fly")) {
                player.fallDistance = 0f;
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        if (ess.getSettings().isWorldChangeSpeedResetEnabled()) {
            if (!user.isAuthorized("essentials.speed")) {
                player.getAbilities().setFlyingSpeed(0.05f);
                player.getAbilities().setWalkingSpeed(0.1f);
                player.onUpdateAbilities();
            } else {
                final float maxFly = (float) ess.getSettings().getMaxFlySpeed() / 2f;
                final float maxWalk = (float) ess.getSettings().getMaxWalkSpeed() / 2f;
                if (player.getAbilities().getFlyingSpeed() > maxFly && !user.isAuthorized("essentials.speed.bypass")) {
                    player.getAbilities().setFlyingSpeed(maxFly);
                }
                if (player.getAbilities().getWalkingSpeed() > maxWalk && !user.isAuthorized("essentials.speed.bypass")) {
                    player.getAbilities().setWalkingSpeed(maxWalk);
                }
                player.onUpdateAbilities();
            }
        }
        if (ess.getSettings().isWorldChangePreserveFlying() && user.isAuthorized("essentials.fly") && user.getFlightTick() != -1) {
            player.getAbilities().mayfly = true;
            if (user.getFlightTick() > 0) {
                player.getAbilities().flying = true;
            }
            player.onUpdateAbilities();
        }
        user.setFlightTick(-1);
        final String newWorld = Worlds.name(destination);
        user.setDisplayNick();
        updateCompass(user);
        if (ess.getSettings().getNoGodWorlds().contains(newWorld) && user.isGodModeEnabledRaw()) {
            user.sendTl("noGodWorldWarning");
        }
        if (!Worlds.name(origin).equals(newWorld)) {
            user.sendTl("currentWorld", newWorld);
        }
        if (user.isVanished()) {
            user.setVanished(user.isAuthorized("essentials.vanish"));
        }
    }

    /** Called before a dimension change (from the teleport path) to remember flight state. */
    public void beforeChangeWorld(final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (ess.getSettings().isWorldChangePreserveFlying() && user.isAuthorized("essentials.fly") && player.getAbilities().mayfly) {
            user.setFlightTick(player.getAbilities().flying ? 1 : 0);
        }
    }

    // ------------------------------------------------------------ chat

    private boolean onChat(final ServerPlayer player, final String message) {
        final User user = ess.getUser(player);
        if (user.isMuted()) {
            user.notifyMuted();
            ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("mutedUserSpeaks", user.getName(), message)));
            return false;
        }
        user.updateActivityOnChat(true);
        user.setDisplayNick();
        return true;
    }

    /**
     * Whether {@code recipient} should receive a chat message from {@code sender}
     * (ignore list; consulted from the chat-send mixin).
     */
    public boolean shouldReceiveChat(final ServerPlayer recipient, final UUID sender) {
        if (sender == null || recipient.getUUID().equals(sender)) {
            return true;
        }
        final User recipientUser = ess.getUsers().getOnlineUserCache().get(recipient.getUUID());
        final User senderUser = ess.getUser(sender);
        if (recipientUser == null || senderUser == null) {
            return true;
        }
        return !recipientUser.isIgnoredPlayer(senderUser);
    }

    // ------------------------------------------------------------ login gates (Section 13.3)

    /**
     * Custom ban messages for the login check. Returns null to keep the vanilla message.
     */
    public Component banMessage(final com.mojang.authlib.GameProfile profile, final String address) {
        final UserBanListEntry banEntry = ess.getServer().getPlayerList().getBans().get(profile);
        if (banEntry != null) {
            final Date banExpiry = banEntry.getExpires();
            if (banExpiry != null) {
                final String expiry = DateUtil.formatDateDiff(banExpiry.getTime());
                return Text.get().mini(tlLiteral("tempbanJoin", expiry, banEntry.getReason()));
            }
            return Text.get().mini(tlLiteral("banJoin", banEntry.getReason()));
        }
        if (address != null) {
            final IpBanListEntry ipEntry = ess.getServer().getPlayerList().getIpBans().get(address);
            if (ipEntry != null) {
                final Date banExpiry = ipEntry.getExpires();
                if (banExpiry != null) {
                    final String expiry = DateUtil.formatDateDiff(banExpiry.getTime());
                    return Text.get().mini(tlLiteral("tempbanIpJoin", expiry, ipEntry.getReason()));
                }
                return Text.get().mini(tlLiteral("banIpJoin", ipEntry.getReason()));
            }
        }
        return null;
    }

    public Component serverFullMessage() {
        return ess.getSettings().isCustomServerFullMessage() ? Text.get().mini(tlLiteral("serverFull")) : null;
    }

    public Component whitelistMessage() {
        return ess.getSettings().isCustomWhitelistMessage() ? Text.get().mini(tlLiteral("whitelistKick")) : null;
    }

    public boolean canBypassFullServer(final UUID uuid) {
        try {
            return ess.getPermissionsHandler().isOfflinePermissionSet(uuid, "essentials.joinfullserver").get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean canBypassWhitelist(final UUID uuid) {
        try {
            return ess.getPermissionsHandler().isOfflinePermissionSet(uuid, "essentials.whitelist.bypass").get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (final Exception e) {
            return false;
        }
    }

    public java.util.Locale localeFor(final ServerPlayer player) {
        return I18n.getLocale(player.clientInformation().language());
    }
}
