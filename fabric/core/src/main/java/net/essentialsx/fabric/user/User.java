package net.essentialsx.fabric.user;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.economy.ImpactorEconomy;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.messaging.SimpleMessageRecipient;
import net.essentialsx.fabric.teleport.AsyncTeleport;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.teleport.TpaRequest;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.TriState;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.essentialsx.fabric.text.I18n.tlLiteral;
import static net.essentialsx.fabric.text.I18n.tlLocale;

/**
 * An Essentials user: persisted record plus session state. May be offline ({@link #getBase()}
 * returns null).
 */
public class User extends UserData implements Comparable<User>, IUser {
    private final IMessageRecipient messageRecipient;
    private final transient AsyncTeleport teleport;
    private final Map<User, BigDecimal> confirmingPayments = new WeakHashMap<>();
    private final transient LinkedHashMap<String, TpaRequest> teleportRequestQueue = new LinkedHashMap<>();
    private transient ServerPlayer base;
    private transient String name;
    private String confirmingClearCommand;
    private String lastHomeConfirmation;
    private transient boolean vanished;
    private boolean hidden = false;
    private boolean leavingHidden = false;
    private boolean rightClickJump = false;
    private boolean invSee = false;
    private boolean recipeSee = false;
    private boolean enderSee = false;
    private boolean ignoreMsg = false;
    private Boolean toggleShout;
    private boolean freeze = false;
    private String afkMessage;
    private long afkSince;
    private transient LazyLocation afkPosition = null;
    private transient long lastOnlineActivity;
    private transient long lastThrottledAction;
    private transient long lastActivity = System.currentTimeMillis();
    private transient long teleportInvulnerabilityTimestamp = 0;
    private long lastNotifiedAboutMailsMs;
    private long lastHomeConfirmationTimestamp;
    private final transient List<String> signCopy = new ArrayList<>(List.of("", "", "", ""));
    private transient long lastVanishTime = System.currentTimeMillis();
    private transient int flightTick = -1;
    private transient long sessionStartTick = -1;
    private transient long lastPlaytimeSaveTick = -1;
    private transient boolean keepInvOnDeath;
    private transient boolean keepXpOnDeath;
    private transient LazyLocation lastKnownPosition;
    private String lastLocaleString;
    private Locale playerLocale;

    public User(final ServerPlayer base, final Essentials ess) {
        super(base.getUUID(), base.getGameProfile().getName(), ess);
        this.base = base;
        this.name = base.getGameProfile().getName();
        teleport = new AsyncTeleport(this, ess);
        if (isAfk()) {
            afkPosition = this.getLocation();
        }
        lastOnlineActivity = System.currentTimeMillis();
        this.messageRecipient = new SimpleMessageRecipient(ess, this);
    }

    public User(final UUID uuid, final String name, final Essentials ess) {
        super(uuid, name, ess);
        this.base = null;
        this.name = name != null ? name : getLastAccountName();
        teleport = new AsyncTeleport(this, ess);
        this.messageRecipient = new SimpleMessageRecipient(ess, this);
    }

    public void update(final ServerPlayer base) {
        this.base = base;
        if (base != null) {
            this.name = base.getGameProfile().getName();
        }
    }

    public void setOffline() {
        this.base = null;
    }

    public Essentials getEssentials() {
        return ess;
    }

    // ---------------------------------------------------------------- base / identity

    @Override
    public ServerPlayer getBase() {
        return base;
    }

    @Override
    public boolean isOnline() {
        return base != null && !base.hasDisconnected();
    }

    @Override
    public String getName() {
        if (base != null) {
            return base.getGameProfile().getName();
        }
        return name != null ? name : (getLastAccountName() != null ? getLastAccountName() : getConfigUUID().toString());
    }

    @Override
    public UUID getUUID() {
        return getConfigUUID();
    }

    @Override
    public LazyLocation getLocation() {
        if (base != null) {
            return LazyLocation.of(base);
        }
        if (getLogoutLocation() != null) {
            return getLogoutLocation();
        }
        return getLastLocation();
    }

    public net.minecraft.server.level.ServerLevel getWorld() {
        return base == null ? null : (net.minecraft.server.level.ServerLevel) base.level();
    }

    public ItemStack getItemInHand() {
        return base == null ? ItemStack.EMPTY : net.essentialsx.fabric.items.Inventories.getItemInHand(base);
    }

    public BlockPos getTargetBlock(final int maxDistance) {
        return base == null ? null : LocationUtil.getTargetBlock(base, maxDistance);
    }

    // ---------------------------------------------------------------- permissions

    public boolean isAuthorized(final net.essentialsx.fabric.command.EssentialsCommand cmd) {
        return isAuthorized(cmd, "essentials.");
    }

    public boolean isAuthorized(final net.essentialsx.fabric.command.EssentialsCommand cmd, final String permissionPrefix) {
        return isAuthorized(permissionPrefix + (cmd.getName().equals("r") ? "msg" : cmd.getName()));
    }

    @Override
    public boolean isAuthorized(final String node) {
        final boolean result = isAuthorizedCheck(node);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("checking if " + getName() + " has " + node + " - " + result);
        }
        return result;
    }

    @Override
    public boolean isAuthorizedCached(final String node) {
        if (base == null) {
            return false;
        }
        try {
            return ess.getPermissionsHandler().hasPermissionCached(base, node);
        } catch (final Exception ex) {
            ess.getLogger().error("Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean isPermissionSet(final String node) {
        if (base == null) {
            return false;
        }
        try {
            return ess.getPermissionsHandler().isPermissionSet(base, node);
        } catch (final Exception ex) {
            return false;
        }
    }

    public TriState isAuthorizedExact(final String node) {
        if (base == null) {
            return TriState.UNSET;
        }
        return ess.getPermissionsHandler().isPermissionSetExact(base, node);
    }

    private boolean isAuthorizedCheck(final String node) {
        if (base == null) {
            return false;
        }
        try {
            return ess.getPermissionsHandler().hasPermission(base, node);
        } catch (final Exception ex) {
            ess.getLogger().error("Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage());
            return false;
        }
    }

    public boolean isOp() {
        return base != null && ess.getServer().getPlayerList().isOp(base.getGameProfile());
    }

    // ---------------------------------------------------------------- heal cooldown

    public void healCooldown() throws Exception {
        final Calendar now = new GregorianCalendar();
        if (getLastHealTimestamp() > 0) {
            final double cooldown = ess.getSettings().getHealCooldown();
            final Calendar cooldownTime = new GregorianCalendar();
            cooldownTime.setTimeInMillis(getLastHealTimestamp());
            cooldownTime.add(Calendar.SECOND, (int) cooldown);
            cooldownTime.add(Calendar.MILLISECOND, (int) ((cooldown * 1000.0) % 1000.0));
            if (cooldownTime.after(now) && !isAuthorized("essentials.heal.cooldown.bypass")) {
                throw new TranslatableException("timeBeforeHeal", DateUtil.formatDateDiff(cooldownTime.getTimeInMillis()));
            }
        }
        setLastHealTimestamp(now.getTimeInMillis());
    }

    // ---------------------------------------------------------------- economy (Impactor-backed)

    private Account account() {
        final ImpactorEconomy eco = ess.getEconomy();
        if (eco == null) {
            return null;
        }
        Account account = eco.cachedAccount(getUUID());
        if (account == null) {
            // Offline user without a loaded account: resolve with a bounded wait off the hot path.
            try {
                account = eco.account(getUUID()).get(5, TimeUnit.SECONDS);
            } catch (final Exception e) {
                ess.getLogger().warn("Could not load Impactor account for {}: {}", getName(), e.getMessage());
                return null;
            }
        }
        return account;
    }

    @Override
    public BigDecimal getMoney() {
        if (ess.getSettings().isEcoDisabled()) {
            return BigDecimal.ZERO;
        }
        final long start = System.nanoTime();
        final Account account = account();
        final BigDecimal value = account == null ? getCachedMoney() : account.balance();
        final long elapsed = System.nanoTime() - start;
        if (elapsed > ess.getSettings().getEconomyLagWarning()) {
            ess.getLogger().info("Lag Notice - Slow Economy Response - Request took over {}ms!", elapsed / 1000000.0);
        }
        updateMoneyCache(value);
        return value;
    }

    public void setMoney(final BigDecimal value) throws Exception {
        if (ess.getSettings().isEcoDisabled()) {
            return;
        }
        final BigDecimal maxMoney = ess.getSettings().getMaxMoney();
        final BigDecimal minMoney = ess.getSettings().getMinMoney();
        if (value.compareTo(maxMoney) > 0) {
            throw new net.essentialsx.fabric.economy.MaxMoneyException();
        }
        BigDecimal newBalance = value;
        if (newBalance.compareTo(minMoney) < 0) {
            newBalance = minMoney;
        }
        final Account account = account();
        if (account == null) {
            throw new TranslatableException("errorWithMessage", "Impactor account unavailable");
        }
        final EconomyTransaction tx = ess.getEconomy().setNow(account, newBalance);
        if (!ImpactorEconomy.success(tx)) {
            throw new TranslatableException("errorWithMessage", "Impactor rejected the transaction: " + tx.result());
        }
        updateMoneyCache(account.balance());
        Trade.log("Update", "Set", "API", getName(), new Trade(newBalance, ess), null, null, null, account.balance(), ess);
    }

    @Override
    public void giveMoney(final BigDecimal value) throws Exception {
        giveMoney(value, null);
    }

    public void giveMoney(final BigDecimal value, final CommandSource initiator) throws Exception {
        if (value.signum() == 0) {
            return;
        }
        final BigDecimal maxMoney = ess.getSettings().getMaxMoney();
        final Account account = account();
        if (account == null) {
            throw new TranslatableException("errorWithMessage", "Impactor account unavailable");
        }
        if (account.balance().add(value).compareTo(maxMoney) > 0) {
            throw new net.essentialsx.fabric.economy.MaxMoneyException();
        }
        final EconomyTransaction tx = ess.getEconomy().depositNow(account, value);
        if (!ImpactorEconomy.success(tx)) {
            throw new TranslatableException("errorWithMessage", "Impactor rejected the deposit: " + tx.result());
        }
        updateMoneyCache(account.balance());
        sendTl("addedToAccount", Text.parsed(NumberUtil.displayCurrency(value, ess)));
        if (initiator != null) {
            initiator.sendTl("addedToOthersAccount", Text.parsed(NumberUtil.displayCurrency(value, ess)), getDisplayName(), Text.parsed(NumberUtil.displayCurrency(account.balance(), ess)));
        }
    }

    public void payUser(final User reciever, final BigDecimal value) throws Exception {
        if (value.compareTo(BigDecimal.ZERO) < 1) {
            throw new Exception(tlLocale(playerLocale, "payMustBePositive"));
        }
        if (canAfford(value)) {
            final Account from = account();
            final Account to = reciever.account();
            if (from == null || to == null) {
                throw new TranslatableException("errorWithMessage", "Impactor account unavailable");
            }
            if (to.balance().add(value).compareTo(ess.getSettings().getMaxMoney()) > 0) {
                throw new net.essentialsx.fabric.economy.MaxMoneyException();
            }
            final var tx = from.transfer(to, ess.getEconomy().normalize(value));
            if (!ImpactorEconomy.success(tx)) {
                if (tx.result() == net.impactdev.impactor.api.economy.transactions.details.EconomyResultType.NOT_ENOUGH_FUNDS) {
                    throw new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(value, ess)));
                }
                throw new TranslatableException("errorWithMessage", "Impactor rejected the transfer: " + tx.result());
            }
            updateMoneyCache(from.balance());
            reciever.updateMoneyCache(to.balance());
            sendTl("moneySentTo", Text.parsed(NumberUtil.displayCurrency(value, ess)), reciever.getDisplayName());
            reciever.sendTl("moneyRecievedFrom", Text.parsed(NumberUtil.displayCurrency(value, ess)), getDisplayName());
            Trade.log("Command", "Pay", "pay", getName(), new Trade(value, ess), reciever.getName(), new Trade(value, ess), getLocation(), from.balance(), ess);
        } else {
            throw new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(value, ess)));
        }
    }

    @Override
    public void takeMoney(final BigDecimal value) {
        takeMoney(value, null);
    }

    public void takeMoney(final BigDecimal value, final CommandSource initiator) {
        if (value.signum() == 0) {
            return;
        }
        final Account account = account();
        if (account == null) {
            ess.getLogger().warn("Impactor account unavailable for {}", getName());
            return;
        }
        final EconomyTransaction tx;
        if (value.signum() < 0) {
            tx = ess.getEconomy().depositNow(account, value.negate());
        } else {
            tx = ess.getEconomy().withdrawNow(account, value);
        }
        if (!ImpactorEconomy.success(tx)) {
            ess.getLogger().warn("Impactor rejected withdrawal of {} from {}: {}", value, getName(), tx.result());
            return;
        }
        updateMoneyCache(account.balance());
        sendTl("takenFromAccount", Text.parsed(NumberUtil.displayCurrency(value, ess)));
        if (initiator != null) {
            initiator.sendTl("takenFromOthersAccount", Text.parsed(NumberUtil.displayCurrency(value, ess)), getDisplayName(), Text.parsed(NumberUtil.displayCurrency(account.balance(), ess)));
        }
    }

    @Override
    public boolean canAfford(final BigDecimal cost) {
        return canAfford(cost, true);
    }

    public boolean canAfford(final BigDecimal cost, final boolean permcheck) {
        if (cost.signum() <= 0) {
            return true;
        }
        final BigDecimal remainingBalance = getMoney().subtract(cost);
        if (!permcheck || isAuthorized("essentials.eco.loan")) {
            return remainingBalance.compareTo(ess.getSettings().getMinMoney()) >= 0;
        }
        return remainingBalance.signum() >= 0;
    }

    public void dispose() {
        ess.runTaskAsynchronously(this::cleanup);
    }

    public Boolean canSpawnItem(final net.minecraft.world.item.Item material) {
        if (ess.getSettings().permissionBasedItemSpawn()) {
            final String itemName = UserData.itemKey(material).toLowerCase(Locale.ENGLISH).replace("_", "").replace(":", "");
            return isAuthorized("essentials.itemspawn.item-all") || isAuthorized("essentials.itemspawn.item-" + itemName);
        }
        return isAuthorized("essentials.itemspawn.exempt") || !ess.getSettings().itemSpawnBlacklist().contains(material);
    }

    // ---------------------------------------------------------------- locations

    @Override
    public void setLastLocation() {
        setLastLocation(this.getLocation());
    }

    @Override
    public void setLastLocationIfChanged(final LazyLocation origin) {
        setLastLocation(origin);
    }

    @Override
    public void setLogoutLocation() {
        if (base != null) {
            setLogoutLocation(LazyLocation.of(base));
        }
    }

    // ---------------------------------------------------------------- teleport requests

    public void requestTeleport(final User player, final boolean here) {
        final TpaRequest request = teleportRequestQueue.getOrDefault(player.getName(), new TpaRequest(player.getName(), player.getUUID()));
        request.setTime(System.currentTimeMillis());
        request.setHere(here);
        request.setLocation(here ? player.getLocation() : this.getLocation());
        teleportRequestQueue.remove(request.getName());
        if (teleportRequestQueue.size() >= ess.getSettings().getTpaMaxRequests()) {
            final List<String> keys = new ArrayList<>(teleportRequestQueue.keySet());
            teleportRequestQueue.remove(keys.get(keys.size() - 1));
        }
        teleportRequestQueue.put(request.getName(), request);
    }

    public Collection<String> getPendingTpaKeys() {
        return teleportRequestQueue.keySet();
    }

    public boolean hasPendingTpaRequests(final boolean inform, final boolean excludeHere) {
        return getNextTpaRequest(inform, false, excludeHere) != null;
    }

    public boolean hasOutstandingTpaRequest(final String playerUsername, final boolean here) {
        final TpaRequest request = getOutstandingTpaRequest(playerUsername, false);
        return request != null && request.isHere() == here;
    }

    public TpaRequest getOutstandingTpaRequest(final String playerUsername, final boolean inform) {
        if (!teleportRequestQueue.containsKey(playerUsername)) {
            return null;
        }
        final long timeout = ess.getSettings().getTpaAcceptCancellation();
        final TpaRequest request = teleportRequestQueue.get(playerUsername);
        if (timeout < 1 || System.currentTimeMillis() - request.getTime() <= timeout * 1000) {
            return request;
        }
        teleportRequestQueue.remove(playerUsername);
        if (inform) {
            final User requester = ess.getUser(request.getRequesterUuid());
            sendTl("requestTimedOutFrom", requester == null ? request.getName() : requester.getDisplayName());
        }
        return null;
    }

    public TpaRequest removeTpaRequest(final String playerUsername) {
        return teleportRequestQueue.remove(playerUsername);
    }

    public TpaRequest getNextTpaRequest(final boolean inform, final boolean ignoreExpirations, final boolean excludeHere) {
        if (teleportRequestQueue.isEmpty()) {
            return null;
        }
        final long timeout = ess.getSettings().getTpaAcceptCancellation();
        final List<String> keys = new ArrayList<>(teleportRequestQueue.keySet());
        java.util.Collections.reverse(keys);
        TpaRequest nextRequest = null;
        for (final String key : keys) {
            final TpaRequest request = teleportRequestQueue.get(key);
            if (timeout < 1 || (System.currentTimeMillis() - request.getTime()) <= TimeUnit.SECONDS.toMillis(timeout)) {
                if (excludeHere && request.isHere()) {
                    continue;
                }
                if (ignoreExpirations) {
                    return request;
                } else if (nextRequest == null) {
                    nextRequest = request;
                }
            } else {
                if (inform) {
                    final User requester = ess.getUser(request.getRequesterUuid());
                    sendTl("requestTimedOutFrom", requester == null ? request.getName() : requester.getDisplayName());
                }
                teleportRequestQueue.remove(key);
            }
        }
        return nextRequest;
    }

    public void clearTpaRequests() {
        teleportRequestQueue.clear();
    }

    // ---------------------------------------------------------------- nicknames / display

    public String getNick() {
        return getNick(true, true);
    }

    public String getNick(final boolean withPrefix, final boolean withSuffix) {
        final StringBuilder prefix = new StringBuilder();
        final String nickname;
        String suffix = "";
        final String nick = getNickname();
        if (ess.getSettings().isCommandDisabled("nick") || nick == null || nick.isEmpty() || nick.equals(getName())) {
            nickname = getName();
        } else if (nick.equalsIgnoreCase(getName())) {
            nickname = nick;
        } else {
            if (isAuthorized("essentials.nick.hideprefix")) {
                nickname = nick;
            } else {
                nickname = FormatUtil.replaceFormat(ess.getSettings().getNicknamePrefix()) + nick;
            }
            suffix = "§r";
        }
        if (isOp()) {
            final String opPrefix = ess.getSettings().getOperatorColor();
            if (opPrefix != null && !opPrefix.isEmpty()) {
                prefix.insert(0, opPrefix);
                suffix = "§r";
            }
        }
        if (ess.getSettings().addPrefixSuffix() && base != null) {
            if (withPrefix || !ess.getSettings().disablePrefix()) {
                final String ptext = FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(base));
                prefix.insert(0, ptext);
                suffix = "§r";
            }
            if (withSuffix || !ess.getSettings().disableSuffix()) {
                final String stext = FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(base));
                suffix = stext + "§r";
                suffix = suffix.replace("§f§r", "§r").replace("§r§r", "§r");
            }
        }
        final String strPrefix = prefix.toString();
        String output = strPrefix + nickname + suffix;
        if (output.charAt(output.length() - 1) == '§') {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    /**
     * Refresh the cached display name used by chat, tab-list and join messages.
     */
    public void setDisplayNick() {
        if (base != null && ess.getSettings().changeDisplayName()) {
            ess.getDisplayNames().update(this);
        }
    }

    /**
     * Legacy-formatted display name (nickname with prefix/suffix) or the real name.
     */
    @Override
    public String getDisplayName() {
        if (ess.getSettings().hideDisplayNameInVanish() && isHidden()) {
            return getName();
        }
        if (!ess.getSettings().changeDisplayName()) {
            return getName();
        }
        final String nick = getNickname();
        if (nick == null || nick.isEmpty() || !isOnline()) {
            if (isOp() && ess.getSettings().getOperatorColor() != null && isOnline()) {
                return getNick(true, true);
            }
            return getName();
        }
        return getNick(true, true);
    }

    @Override
    public String getFormattedNickname() {
        final String rawNickname = getNickname();
        if (rawNickname == null) {
            return null;
        }
        return FormatUtil.replaceFormat(ess.getSettings().getNicknamePrefix() + rawNickname);
    }

    @Override
    public AsyncTeleport getAsyncTeleport() {
        return teleport;
    }

    public long getLastOnlineActivity() {
        return lastOnlineActivity;
    }

    public void setLastOnlineActivity(final long timestamp) {
        lastOnlineActivity = timestamp;
    }

    // ---------------------------------------------------------------- AFK

    public void setAfk(final boolean set) {
        setAfk(set, null);
    }

    public void setAfk(final boolean set, final Object cause) {
        if (set && !isAfk()) {
            afkPosition = this.getLocation();
            this.afkSince = System.currentTimeMillis();
        } else if (!set && isAfk()) {
            afkPosition = null;
            this.afkMessage = null;
            this.afkSince = 0;
        }
        _setAfk(set);
        ess.getSleepManager().update(this);
        updateAfkListName();
    }

    private void updateAfkListName() {
        if (ess.getSettings().isAfkListName()) {
            ess.getDisplayNames().update(this);
        }
    }

    public boolean toggleAfk(final Object cause) {
        setAfk(!isAfk(), cause);
        return isAfk();
    }

    @Override
    public boolean isHiddenFrom(final ServerPlayer player) {
        if (base == null || player == null) {
            return true;
        }
        return !ess.getVisibility().canSee(player, base);
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public boolean isLeavingHidden() {
        return leavingHidden;
    }

    public void setLeavingHidden(final boolean leavingHidden) {
        this.leavingHidden = leavingHidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
        if (hidden) {
            setLastLogout(getLastOnlineActivity());
        }
    }

    public boolean isHidden(final ServerPlayer player) {
        return hidden || isHiddenFrom(player);
    }

    public String getFormattedJailTime() {
        return DateUtil.formatDateDiff(getOnlineJailedTime() > 0 ? getOnlineJailExpireTime() : getJailTimeout());
    }

    private long getOnlineJailExpireTime() {
        return ((getOnlineJailedTime() - getPlaytimeTicksLive()) * 50) + System.currentTimeMillis();
    }

    public boolean checkJailTimeout(final long currentTime) {
        if (getJailTimeout() > 0) {
            if (getOnlineJailedTime() > 0) {
                if (getOnlineJailedTime() > getPlaytimeTicksLive()) {
                    return false;
                }
            }
            if (getJailTimeout() < currentTime && isJailed()) {
                setJailTimeout(0);
                setOnlineJailedTime(0);
                setJailed(false);
                sendTl("haveBeenReleased");
                setJail(null);
                if (isOnline()) {
                    if (ess.getSettings().getTeleportWhenFreePolicy() == net.essentialsx.fabric.config.Settings.TeleportWhenFreePolicy.BACK) {
                        final CompletableFuture<Boolean> future = new CompletableFuture<>();
                        future.exceptionally(e -> {
                            getAsyncTeleport().respawn(null, TeleportCause.PLUGIN, new CompletableFuture<>());
                            return false;
                        });
                        getAsyncTeleport().back(future);
                    } else if (ess.getSettings().getTeleportWhenFreePolicy() == net.essentialsx.fabric.config.Settings.TeleportWhenFreePolicy.SPAWN) {
                        getAsyncTeleport().respawn(null, TeleportCause.PLUGIN, new CompletableFuture<>());
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean checkMuteTimeout(final long currentTime) {
        if (getMuteTimeout() > 0 && getMuteTimeout() < currentTime && isMuted()) {
            setMuteTimeout(0);
            sendTl("canTalkAgain");
            setMuted(false);
            setMuteReason(null);
            return true;
        }
        return false;
    }

    public void notifyMuted() {
        final String dateDiff = getMuteTimeout() > 0 ? DateUtil.formatDateDiff(getMuteTimeout()) : null;
        if (dateDiff == null) {
            if (hasMuteReason()) {
                sendTl("voiceSilencedReason", getMuteReason());
            } else {
                sendTl("voiceSilenced");
            }
        } else {
            if (hasMuteReason()) {
                sendTl("voiceSilencedReasonTime", dateDiff, getMuteReason());
            } else {
                sendTl("voiceSilencedTime", dateDiff);
            }
        }
    }

    public long getLastActivityTime() {
        return this.lastActivity;
    }

    public void updateActivity(final boolean broadcast, final Object cause) {
        if (isAfk()) {
            setAfk(false, cause);
            if (broadcast && !isHidden() && !isAfk()) {
                setDisplayNick();
                if (ess.getSettings().broadcastAfkMessage()) {
                    ess.broadcastTl(this, u -> u == this, "userIsNotAway", getDisplayName());
                }
                sendTl("userIsNotAwaySelf", getDisplayName());
            }
        }
        lastActivity = System.currentTimeMillis();
    }

    public void updateActivityOnMove(final boolean broadcast) {
        if (ess.getSettings().cancelAfkOnMove()) {
            updateActivity(broadcast, "MOVE");
        }
    }

    public void updateActivityOnInteract(final boolean broadcast) {
        if (ess.getSettings().cancelAfkOnInteract()) {
            updateActivity(broadcast, "INTERACT");
        }
    }

    public void updateActivityOnChat(final boolean broadcast) {
        if (ess.getSettings().cancelAfkOnChat()) {
            ess.scheduleSyncDelayedTask(() -> updateActivity(broadcast, "CHAT"));
        }
    }

    public void checkActivity() {
        if (System.currentTimeMillis() - lastActivity <= 10000) {
            return;
        }
        final long autoafktimeout = ess.getSettings().getAutoAfkTimeout();
        if (autoafktimeout > 0
            && lastActivity > 0 && (lastActivity + (autoafktimeout * 1000)) < System.currentTimeMillis()
            && !isAuthorized("essentials.kick.exempt")
            && !isAuthorized("essentials.afk.kickexempt")) {
            lastActivity = 0;
            final double kickTime = autoafktimeout / 60.0;
            if (ess.getSettings().getAfkTimeoutCommands().isEmpty()) {
                final String reason = playerTl("autoAfkKickReason", kickTime);
                ess.kickPlayer(this, reason);
                for (final User user : ess.getOnlineUsers()) {
                    if (user.isAuthorized("essentials.kick.notify")) {
                        user.sendTl("playerKicked", Console.displayName(), getName(), user.playerTl("autoAfkKickReason", kickTime));
                    }
                }
            } else {
                for (final String command : ess.getSettings().getAfkTimeoutCommands()) {
                    if (command == null || command.isEmpty()) {
                        continue;
                    }
                    final String cmd = command.replace("{USERNAME}", getName()).replace("{KICKTIME}", String.valueOf(kickTime));
                    ess.dispatchConsoleCommand(cmd);
                }
            }
        }
        final long autoafk = ess.getSettings().getAutoAfk();
        if (!isAfk() && autoafk > 0 && lastActivity + autoafk * 1000 < System.currentTimeMillis() && isAuthorizedCached("essentials.afk.auto")) {
            setAfk(true, "ACTIVITY");
            if (isAfk() && !isHidden()) {
                setDisplayNick();
                if (ess.getSettings().broadcastAfkMessage()) {
                    ess.broadcastTl(this, u -> u == this, "userIsAway", getDisplayName());
                }
                sendTl("userIsAwaySelf", getDisplayName());
            }
        }
    }

    public LazyLocation getAfkPosition() {
        return afkPosition;
    }

    @Override
    public boolean isGodModeEnabled() {
        if (super.isGodModeEnabled()) {
            final LazyLocation loc = getLocation();
            if (loc == null || !ess.getSettings().getNoGodWorlds().contains(loc.worldDisplayName(ess.getServer()))) {
                return true;
            }
        }
        if (isAfk()) {
            return ess.getSettings().getFreezeAfkPlayers();
        }
        return false;
    }

    public boolean isGodModeEnabledRaw() {
        return super.isGodModeEnabled();
    }

    public String getGroup() {
        if (base == null) {
            return "default";
        }
        final String result = ess.getPermissionsHandler().getGroup(base);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("looking up groupname of " + getName() + " - " + result);
        }
        return result;
    }

    public boolean inGroup(final String group) {
        if (base == null) {
            return false;
        }
        return ess.getPermissionsHandler().inGroup(base, group);
    }

    public boolean canBuild() {
        return true;
    }

    public boolean isInvSee() {
        return invSee;
    }

    public void setInvSee(final boolean set) {
        invSee = set;
    }

    public boolean isEnderSee() {
        return enderSee;
    }

    public void setEnderSee(final boolean set) {
        enderSee = set;
    }

    @Override
    public void enableInvulnerabilityAfterTeleport() {
        final long time = ess.getSettings().getTeleportInvulnerability();
        if (time > 0) {
            teleportInvulnerabilityTimestamp = System.currentTimeMillis() + time;
        }
    }

    @Override
    public void resetInvulnerabilityAfterTeleport() {
        if (teleportInvulnerabilityTimestamp != 0 && teleportInvulnerabilityTimestamp < System.currentTimeMillis()) {
            teleportInvulnerabilityTimestamp = 0;
        }
    }

    @Override
    public boolean hasInvulnerabilityAfterTeleport() {
        return teleportInvulnerabilityTimestamp != 0 && teleportInvulnerabilityTimestamp >= System.currentTimeMillis();
    }

    public boolean canInteractVanished() {
        return isAuthorized("essentials.vanish.interact");
    }

    @Override
    public boolean isIgnoreMsg() {
        return ignoreMsg;
    }

    @Override
    public void setIgnoreMsg(final boolean ignoreMsg) {
        this.ignoreMsg = ignoreMsg;
    }

    @Override
    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(final boolean set) {
        vanished = set;
        if (base == null) {
            setHidden(set);
            return;
        }
        if (set) {
            setHidden(true);
            lastVanishTime = System.currentTimeMillis();
            ess.getVanishedPlayersNew().add(getName());
            if (isAuthorized("essentials.vanish.effect")) {
                base.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
            }
        } else {
            setHidden(false);
            ess.getVanishedPlayersNew().remove(getName());
            if (isAuthorized("essentials.vanish.effect")) {
                base.removeEffect(MobEffects.INVISIBILITY);
            }
        }
        ess.getVisibility().onVanishChanged(this, set);
        ess.getSleepManager().update(this);
    }

    public boolean checkSignThrottle() {
        if (isSignThrottled()) {
            return true;
        }
        updateThrottle();
        return false;
    }

    public boolean isSignThrottled() {
        final long minTime = lastThrottledAction + (1000 / ess.getSettings().getSignUsePerSecond());
        return System.currentTimeMillis() < minTime;
    }

    public void updateThrottle() {
        lastThrottledAction = System.currentTimeMillis();
    }

    public boolean isFlyClickJump() {
        return rightClickJump;
    }

    public void setRightClickJump(final boolean rightClickJump) {
        this.rightClickJump = rightClickJump;
    }

    @Override
    public boolean isIgnoreExempt() {
        return this.isAuthorized("essentials.chat.ignoreexempt");
    }

    public boolean isRecipeSee() {
        return recipeSee;
    }

    public void setRecipeSee(final boolean recipeSee) {
        this.recipeSee = recipeSee;
    }

    // ---------------------------------------------------------------- messaging

    @Override
    public void sendMessage(final String message) {
        if (base != null && !message.isEmpty()) {
            base.sendSystemMessage(Text.get().legacy(message));
        }
    }

    @Override
    public void sendComponent(final net.kyori.adventure.text.Component component) {
        if (base != null) {
            base.sendSystemMessage(Text.get().toNative(component));
        }
    }

    @Override
    public void sendNative(final net.minecraft.network.chat.Component component) {
        if (base != null) {
            base.sendSystemMessage(component);
        }
    }

    @Override
    public net.kyori.adventure.text.Component tlComponent(final String tlKey, final Object... args) {
        final String translation = playerTl(tlKey, args);
        return Text.get().deserializeMiniMessage(translation);
    }

    @Override
    public void sendTl(final String tlKey, final Object... args) {
        if (base == null) {
            return;
        }
        final String translation = playerTl(tlKey, args);
        if (translation.trim().isEmpty()) {
            return;
        }
        sendComponent(Text.get().deserializeMiniMessage(translation));
    }

    @Override
    public String playerTl(final String tlKey, final Object... args) {
        if (ess.getSettings().isPerPlayerLocale()) {
            final Locale locale = base != null ? getPlayerLocale(base.clientInformation().language()) : playerLocale;
            if (locale != null) {
                return tlLocale(locale, tlKey, args);
            }
        }
        return tlLiteral(tlKey, args);
    }

    @Override
    public String tlSender(final String tlKey, final Object... args) {
        return playerTl(tlKey, args);
    }

    public Locale getPlayerLocale(final String locale) {
        if (locale == null || locale.equals(lastLocaleString)) {
            return playerLocale;
        }
        lastLocaleString = locale;
        return playerLocale = I18n.getLocale(locale);
    }

    @Override
    public int compareTo(final User other) {
        return FormatUtil.stripFormat(getDisplayName()).compareToIgnoreCase(FormatUtil.stripFormat(other.getDisplayName()));
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof User)) {
            return false;
        }
        return this.getUUID().equals(((User) object).getUUID());
    }

    @Override
    public int hashCode() {
        return this.getUUID().hashCode();
    }

    @Override
    public CommandSource getSource() {
        if (base != null) {
            return new CommandSource(ess, base.createCommandSourceStack());
        }
        return new CommandSource(ess, ess.getServer().createCommandSourceStack());
    }

    @Override
    public boolean isReachable() {
        return isOnline();
    }

    @Override
    public IMessageRecipient.MessageResponse sendMessage(final IMessageRecipient recipient, final String message) {
        return this.messageRecipient.sendMessage(recipient, message);
    }

    @Override
    public IMessageRecipient.MessageResponse onReceiveMessage(final IMessageRecipient sender, final String message) {
        return this.messageRecipient.onReceiveMessage(sender, message);
    }

    @Override
    public IMessageRecipient getReplyRecipient() {
        return this.messageRecipient.getReplyRecipient();
    }

    @Override
    public void setReplyRecipient(final IMessageRecipient recipient) {
        this.messageRecipient.setReplyRecipient(recipient);
    }

    @Override
    public String getAfkMessage() {
        return this.afkMessage;
    }

    @Override
    public void setAfkMessage(final String message) {
        if (isAfk()) {
            this.afkMessage = message;
        }
    }

    @Override
    public long getAfkSince() {
        return afkSince;
    }

    public Map<User, BigDecimal> getConfirmingPayments() {
        return confirmingPayments;
    }

    public String getConfirmingClearCommand() {
        return confirmingClearCommand;
    }

    public void setConfirmingClearCommand(final String command) {
        this.confirmingClearCommand = command;
    }

    @Override
    public void sendMail(final IMessageRecipient sender, final String message) {
        sendMail(sender, message, 0);
    }

    @Override
    public void sendMail(final IMessageRecipient sender, final String message, final long expireAt) {
        ess.getMail().sendMail(this, sender, message, expireAt);
    }

    public void notifyOfMail() {
        final int unread = getUnreadMailAmount();
        if (unread != 0) {
            final int notifyPlayerOfMailCooldown = ess.getSettings().getNotifyPlayerOfMailCooldown() * 1000;
            if (System.currentTimeMillis() - lastNotifiedAboutMailsMs >= notifyPlayerOfMailCooldown) {
                sendTl("youHaveNewMail", unread);
                lastNotifiedAboutMailsMs = System.currentTimeMillis();
            }
        }
    }

    public String getLastHomeConfirmation() {
        return lastHomeConfirmation;
    }

    public void setLastHomeConfirmation(final String lastHomeConfirmation) {
        this.lastHomeConfirmation = lastHomeConfirmation;
    }

    public long getLastHomeConfirmationTimestamp() {
        return lastHomeConfirmationTimestamp;
    }

    public void setLastHomeConfirmationTimestamp() {
        this.lastHomeConfirmationTimestamp = System.currentTimeMillis();
    }

    public List<String> getSignCopy() {
        return signCopy;
    }

    @Override
    public boolean isFreeze() {
        return freeze;
    }

    @Override
    public void setFreeze(final boolean freeze) {
        this.freeze = freeze;
    }

    public boolean isBaltopExempt() {
        if (isOnline()) {
            final boolean exempt = isAuthorized("essentials.balancetop.exclude");
            setBaltopExemptCache(exempt);
            return exempt;
        }
        return isBaltopExcludeCache();
    }

    public long getLastVanishTime() {
        return lastVanishTime;
    }

    public void setToggleShout(final boolean toggleShout) {
        this.toggleShout = toggleShout;
        if (ess.getSettings().isPersistShout()) {
            setShouting(toggleShout);
        }
    }

    public boolean isToggleShout() {
        if (ess.getSettings().isPersistShout()) {
            return toggleShout = isShouting();
        }
        return toggleShout == null ? toggleShout = ess.getSettings().isShoutDefault() : toggleShout;
    }

    public int getFlightTick() {
        return flightTick;
    }

    public void setFlightTick(final int flightTick) {
        this.flightTick = flightTick;
    }

    // ---------------------------------------------------------------- playtime (ticks online, persisted)

    public void onSessionStart(final long serverTick) {
        sessionStartTick = serverTick;
        lastPlaytimeSaveTick = serverTick;
    }

    /** Playtime in ticks including the current session. */
    public long getPlaytimeTicksLive() {
        if (sessionStartTick < 0 || ess.getServer() == null) {
            return getPlaytimeTicks();
        }
        return getPlaytimeTicks() + (ess.getServer().getTickCount() - lastPlaytimeSaveTick);
    }

    public void flushPlaytime(final long serverTick) {
        if (lastPlaytimeSaveTick >= 0) {
            addPlaytimeTicks(serverTick - lastPlaytimeSaveTick);
            lastPlaytimeSaveTick = serverTick;
        }
    }

    public void onSessionEnd(final long serverTick) {
        flushPlaytime(serverTick);
        sessionStartTick = -1;
        lastPlaytimeSaveTick = -1;
        save();
    }

    // ---------------------------------------------------------------- death bookkeeping

    public boolean isKeepInvOnDeath() {
        return keepInvOnDeath;
    }

    public void setKeepInvOnDeath(final boolean keepInvOnDeath) {
        this.keepInvOnDeath = keepInvOnDeath;
    }

    public boolean isKeepXpOnDeath() {
        return keepXpOnDeath;
    }

    public void setKeepXpOnDeath(final boolean keepXpOnDeath) {
        this.keepXpOnDeath = keepXpOnDeath;
    }

    public LazyLocation getLastKnownPosition() {
        return lastKnownPosition;
    }

    public void setLastKnownPosition(final LazyLocation lastKnownPosition) {
        this.lastKnownPosition = lastKnownPosition;
    }

    public boolean isGameMode(final GameType type) {
        return base != null && base.gameMode.getGameModeForPlayer() == type;
    }

    public String getIpAddress() {
        return base == null ? getLastLoginAddress() : base.getIpAddress();
    }
}
