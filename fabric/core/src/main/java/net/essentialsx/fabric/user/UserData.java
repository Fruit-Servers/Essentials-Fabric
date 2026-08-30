package net.essentialsx.fabric.user;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Persistent user record stored at {@code userdata/<uuid>.yml} using the upstream
 * EssentialsX key layout so existing data can be imported unchanged.
 * All persisted fields live in memory; saves encode a snapshot off-thread.
 */
public abstract class UserData {
    protected final transient Essentials ess;
    private final YamlFile config;
    private final UUID uuid;

    // --- persisted fields (upstream UserConfigHolder) ---
    private BigDecimal money;
    private final Map<String, LazyLocation> homes = new LinkedHashMap<>();
    private String nickname;
    private final Set<String> unlimited = new HashSet<>();
    private final Map<String, List<String>> powertools = new LinkedHashMap<>();
    private LazyLocation lastLocation;
    private LazyLocation logoutLocation;
    private String jail;
    private final List<MailMessage> mail = new ArrayList<>();
    private boolean teleportEnabled = true;
    private boolean teleportAuto = false;
    private final List<UUID> ignore = new ArrayList<>();
    private boolean godMode = false;
    private boolean flyMode = false;
    private boolean muted = false;
    private String muteReason;
    private boolean jailed = false;
    private String ipAddress = "";
    private boolean afk = false;
    private String geolocation;
    private boolean socialSpy = false;
    private boolean npc = false;
    private String lastAccountName;
    private String npcName;
    private boolean powerToolsEnabled = true;
    private boolean acceptingPay = true;
    private Boolean confirmPay;
    private Boolean confirmClear;
    private Boolean lastMessageReplyRecipient;
    private boolean baltopExempt = false;
    private Boolean shouting;
    private final List<String> pastUsernames = new ArrayList<>();
    // timestamps
    private long lastTeleport;
    private long lastHeal;
    private long muteTimeout;
    private long jailTimeout;
    private long onlineJail;
    private long logout;
    private long login;
    private final Map<String, Long> kitTimestamps = new LinkedHashMap<>();
    private final List<CommandCooldown> commandCooldowns = new ArrayList<>();
    // playtime tracked by the mod (ticks online)
    private long playtimeTicks;
    // essentials-fabric import marker
    private Map<String, Object> economyImport;
    /** Unknown keys preserved verbatim. */
    private final Map<String, Object> extensions = new LinkedHashMap<>();
    private Map<String, Object> info = new LinkedHashMap<>();

    protected UserData(final UUID uuid, final String name, final Essentials ess) {
        this.ess = ess;
        this.uuid = uuid;
        final Path folder = ess.getDataFolder().resolve("userdata");
        try {
            Files.createDirectories(folder);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to create userdata folder!", e);
        }
        config = new YamlFile(folder.resolve(uuid + ".yml"));
        config.setSaveHook(this::writeToConfig);
        reloadConfig();
        if (lastAccountName == null && name != null) {
            lastAccountName = name;
        }
    }

    public final YamlFile getConfig() {
        return config;
    }

    public final void reset() {
        config.blockingSave();
        try {
            Files.deleteIfExists(config.getFile());
        } catch (final Exception e) {
            ess.getLogger().warn("Unable to delete data file for " + config.getFile().getFileName());
        }
        ess.getUsers().invalidate(uuid);
    }

    public final void cleanup() {
        config.blockingSave();
        ess.getUsers().removeCache(uuid);
    }

    public final void reloadConfig() {
        config.load();
        readFromConfig(config.getRoot());
        money = _getMoney();
    }

    // ---------------------------------------------------------------- (de)serialization

    private static final Set<String> KNOWN_KEYS = Set.of("money", "homes", "nickname", "unlimited", "powertools", "lastlocation", "logoutlocation",
        "jail", "mail", "teleportenabled", "teleportauto", "ignore", "godmode", "flymode", "muted", "muteReason", "jailed", "ipAddress",
        "afk", "geolocation", "socialspy", "npc", "lastAccountName", "npcName", "powertoolsenabled", "acceptingPay", "confirmPay",
        "confirmClear", "lastMessageReplyRecipient", "baltopExempt", "shouting", "pastUsernames", "timestamps", "info", "playtime", "economy", "schema");

    @SuppressWarnings("unchecked")
    private void readFromConfig(final Map<String, Object> root) {
        homes.clear();
        unlimited.clear();
        powertools.clear();
        mail.clear();
        ignore.clear();
        pastUsernames.clear();
        kitTimestamps.clear();
        commandCooldowns.clear();
        extensions.clear();

        money = root.get("money") == null ? null : bigDecimal(root.get("money"));
        final Object homesObj = root.get("homes");
        if (homesObj instanceof Map) {
            for (final Map.Entry<String, Object> e : ((Map<String, Object>) homesObj).entrySet()) {
                if (e.getValue() instanceof Map) {
                    final LazyLocation loc = LazyLocation.fromMap((Map<String, Object>) e.getValue());
                    if (loc != null) {
                        homes.put(e.getKey(), loc);
                    }
                }
            }
        }
        nickname = str(root.get("nickname"));
        if (root.get("unlimited") instanceof List) {
            for (final Object o : (List<Object>) root.get("unlimited")) {
                if (o != null) {
                    unlimited.add(normalizeItemKey(o.toString()));
                }
            }
        }
        if (root.get("powertools") instanceof Map) {
            for (final Map.Entry<String, Object> e : ((Map<String, Object>) root.get("powertools")).entrySet()) {
                final List<String> list = new ArrayList<>();
                if (e.getValue() instanceof List) {
                    for (final Object o : (List<Object>) e.getValue()) {
                        if (o != null) {
                            list.add(o.toString());
                        }
                    }
                } else if (e.getValue() != null) {
                    list.add(e.getValue().toString());
                }
                powertools.put(normalizeItemKey(e.getKey()), list);
            }
        }
        lastLocation = root.get("lastlocation") instanceof Map ? LazyLocation.fromMap((Map<String, Object>) root.get("lastlocation")) : null;
        logoutLocation = root.get("logoutlocation") instanceof Map ? LazyLocation.fromMap((Map<String, Object>) root.get("logoutlocation")) : null;
        jail = str(root.get("jail"));
        if (root.get("mail") instanceof List) {
            for (final Object o : (List<Object>) root.get("mail")) {
                if (o instanceof Map) {
                    mail.add(MailMessage.fromMap((Map<String, Object>) o));
                } else if (o != null) {
                    mail.add(MailMessage.fromLegacy(o.toString()));
                }
            }
        }
        teleportEnabled = bool(root.get("teleportenabled"), true);
        teleportAuto = bool(root.get("teleportauto"), false);
        if (root.get("ignore") instanceof List) {
            for (final Object o : (List<Object>) root.get("ignore")) {
                final UUID id = StringUtil.toUUID(String.valueOf(o));
                if (id != null) {
                    ignore.add(id);
                }
            }
        }
        godMode = bool(root.get("godmode"), false);
        flyMode = bool(root.get("flymode"), false);
        muted = bool(root.get("muted"), false);
        muteReason = str(root.get("muteReason"));
        jailed = bool(root.get("jailed"), false);
        ipAddress = root.get("ipAddress") == null ? "" : root.get("ipAddress").toString();
        afk = bool(root.get("afk"), false);
        geolocation = str(root.get("geolocation"));
        socialSpy = bool(root.get("socialspy"), false);
        npc = bool(root.get("npc"), false);
        lastAccountName = str(root.get("lastAccountName"));
        npcName = str(root.get("npcName"));
        powerToolsEnabled = bool(root.get("powertoolsenabled"), true);
        acceptingPay = bool(root.get("acceptingPay"), true);
        confirmPay = root.get("confirmPay") instanceof Boolean ? (Boolean) root.get("confirmPay") : null;
        confirmClear = root.get("confirmClear") instanceof Boolean ? (Boolean) root.get("confirmClear") : null;
        lastMessageReplyRecipient = root.get("lastMessageReplyRecipient") instanceof Boolean ? (Boolean) root.get("lastMessageReplyRecipient") : null;
        baltopExempt = bool(root.get("baltopExempt"), false);
        shouting = root.get("shouting") instanceof Boolean ? (Boolean) root.get("shouting") : null;
        if (root.get("pastUsernames") instanceof List) {
            for (final Object o : (List<Object>) root.get("pastUsernames")) {
                if (o != null) {
                    pastUsernames.add(o.toString());
                }
            }
        }
        if (root.get("timestamps") instanceof Map) {
            final Map<String, Object> ts = (Map<String, Object>) root.get("timestamps");
            lastTeleport = lng(ts.get("lastteleport"));
            lastHeal = lng(ts.get("lastheal"));
            muteTimeout = lng(ts.get("mute"));
            jailTimeout = lng(ts.get("jail"));
            onlineJail = lng(ts.get("onlinejail"));
            logout = lng(ts.get("logout"));
            login = lng(ts.get("login"));
            if (ts.get("kits") instanceof Map) {
                for (final Map.Entry<String, Object> e : ((Map<String, Object>) ts.get("kits")).entrySet()) {
                    kitTimestamps.put(e.getKey(), lng(e.getValue()));
                }
            }
            if (ts.get("commandCooldowns") instanceof List) {
                for (final Object o : (List<Object>) ts.get("commandCooldowns")) {
                    if (o instanceof Map) {
                        final CommandCooldown cd = CommandCooldown.fromMap((Map<String, Object>) o);
                        if (cd != null) {
                            commandCooldowns.add(cd);
                        }
                    }
                }
            }
        }
        playtimeTicks = lng(root.get("playtime"));
        economyImport = root.get("economy") instanceof Map ? new LinkedHashMap<>((Map<String, Object>) root.get("economy")) : null;
        info = root.get("info") instanceof Map ? new LinkedHashMap<>((Map<String, Object>) root.get("info")) : new LinkedHashMap<>();
        for (final Map.Entry<String, Object> e : root.entrySet()) {
            if (!KNOWN_KEYS.contains(e.getKey())) {
                extensions.put(e.getKey(), e.getValue());
            }
        }
    }

    private void writeToConfig() {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 3);
        if (money != null) {
            root.put("money", money.toPlainString());
        }
        if (!homes.isEmpty()) {
            final Map<String, Object> h = new LinkedHashMap<>();
            for (final Map.Entry<String, LazyLocation> e : homes.entrySet()) {
                h.put(e.getKey(), e.getValue().toMap());
            }
            root.put("homes", h);
        }
        if (nickname != null) {
            root.put("nickname", nickname);
        }
        if (!unlimited.isEmpty()) {
            root.put("unlimited", new ArrayList<>(unlimited));
        }
        if (!powertools.isEmpty()) {
            root.put("powertools", new LinkedHashMap<>(powertools));
        }
        if (lastLocation != null) {
            root.put("lastlocation", lastLocation.toMap());
        }
        if (logoutLocation != null) {
            root.put("logoutlocation", logoutLocation.toMap());
        }
        if (jail != null) {
            root.put("jail", jail);
        }
        if (!mail.isEmpty()) {
            final List<Object> list = new ArrayList<>();
            for (final MailMessage m : mail) {
                list.add(m.toMap());
            }
            root.put("mail", list);
        }
        root.put("teleportenabled", teleportEnabled);
        root.put("teleportauto", teleportAuto);
        if (!ignore.isEmpty()) {
            final List<String> list = new ArrayList<>();
            for (final UUID id : ignore) {
                list.add(id.toString());
            }
            root.put("ignore", list);
        }
        root.put("godmode", godMode);
        root.put("flymode", flyMode);
        root.put("muted", muted);
        if (muteReason != null) {
            root.put("muteReason", muteReason);
        }
        root.put("jailed", jailed);
        root.put("ipAddress", ipAddress == null ? "" : ipAddress);
        root.put("afk", afk);
        if (geolocation != null) {
            root.put("geolocation", geolocation);
        }
        root.put("socialspy", socialSpy);
        root.put("npc", npc);
        if (lastAccountName != null) {
            root.put("lastAccountName", lastAccountName);
        }
        if (npcName != null) {
            root.put("npcName", npcName);
        }
        root.put("powertoolsenabled", powerToolsEnabled);
        root.put("acceptingPay", acceptingPay);
        if (confirmPay != null) {
            root.put("confirmPay", confirmPay);
        }
        if (confirmClear != null) {
            root.put("confirmClear", confirmClear);
        }
        if (lastMessageReplyRecipient != null) {
            root.put("lastMessageReplyRecipient", lastMessageReplyRecipient);
        }
        root.put("baltopExempt", baltopExempt);
        if (shouting != null) {
            root.put("shouting", shouting);
        }
        if (!pastUsernames.isEmpty()) {
            root.put("pastUsernames", new ArrayList<>(pastUsernames));
        }
        final Map<String, Object> ts = new LinkedHashMap<>();
        ts.put("lastteleport", lastTeleport);
        ts.put("lastheal", lastHeal);
        ts.put("mute", muteTimeout);
        ts.put("jail", jailTimeout);
        ts.put("onlinejail", onlineJail);
        ts.put("logout", logout);
        ts.put("login", login);
        if (!kitTimestamps.isEmpty()) {
            ts.put("kits", new LinkedHashMap<>(kitTimestamps));
        }
        if (!commandCooldowns.isEmpty()) {
            final List<Object> list = new ArrayList<>();
            for (final CommandCooldown cd : commandCooldowns) {
                list.add(cd.toMap());
            }
            ts.put("commandCooldowns", list);
        }
        root.put("timestamps", ts);
        if (playtimeTicks > 0) {
            root.put("playtime", playtimeTicks);
        }
        if (economyImport != null) {
            root.put("economy", economyImport);
        }
        if (!info.isEmpty()) {
            root.put("info", info);
        }
        root.putAll(extensions);
        config.setRoot(root);
    }

    private static String str(final Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean bool(final Object o, final boolean def) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof String) {
            return Boolean.parseBoolean((String) o);
        }
        return def;
    }

    private static long lng(final Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (final NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private static BigDecimal bigDecimal(final Object o) {
        try {
            if (o instanceof BigDecimal) {
                return (BigDecimal) o;
            }
            if (o instanceof Double || o instanceof Float) {
                return BigDecimal.valueOf(((Number) o).doubleValue());
            }
            return new BigDecimal(o.toString().trim());
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Item keys are stored as lowercase registry paths ({@code diamond_sword}); legacy
     * Bukkit material names ({@code DIAMOND_SWORD}) map onto the same form.
     */
    public static String normalizeItemKey(final String key) {
        String k = key.toLowerCase(Locale.ENGLISH);
        if (k.startsWith("minecraft:")) {
            k = k.substring("minecraft:".length());
        }
        return k;
    }

    public static String itemKey(final ItemStack stack) {
        return itemKey(stack.getItem());
    }

    public static String itemKey(final Item item) {
        final ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        if (loc.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return loc.getPath();
        }
        return loc.toString();
    }

    // ---------------------------------------------------------------- money (cached, non-authoritative)

    private BigDecimal _getMoney() {
        BigDecimal result = ess.getSettings().getStartingBalance();
        final BigDecimal maxMoney = ess.getSettings().getMaxMoney();
        final BigDecimal minMoney = ess.getSettings().getMinMoney();
        if (isNPC()) {
            result = BigDecimal.ZERO;
        }
        if (money != null) {
            result = money;
        }
        if (result.compareTo(maxMoney) > 0) {
            result = maxMoney;
        }
        if (result.compareTo(minMoney) < 0) {
            result = minMoney;
        }
        return result;
    }

    /**
     * Balance as last imported/observed. The authoritative balance lives in Impactor.
     */
    public BigDecimal getCachedMoney() {
        return money == null ? _getMoney() : money;
    }

    public boolean hasImportedBalance() {
        return money != null;
    }

    public void updateMoneyCache(final BigDecimal value) {
        this.money = value;
    }

    public Map<String, Object> getEconomyImportMarker() {
        return economyImport;
    }

    public void setEconomyImportMarker(final Map<String, Object> marker) {
        this.economyImport = marker;
        config.save();
    }

    // ---------------------------------------------------------------- homes

    private String getHomeName(String search) {
        if (NumberUtil.isInt(search)) {
            try {
                search = getHomes().get(Integer.parseInt(search) - 1);
            } catch (final NumberFormatException | IndexOutOfBoundsException ignored) {
            }
        }
        return search;
    }

    public LazyLocation getHome(final String name) {
        final String search = getHomeName(name);
        return homes.get(search);
    }

    public boolean hasValidHomes() {
        for (final LazyLocation loc : homes.values()) {
            if (loc != null && loc.isAvailable(ess.getServer())) {
                return true;
            }
        }
        return false;
    }

    public LazyLocation getHome(final LazyLocation world) {
        if (homes.isEmpty()) {
            return null;
        }
        for (final String home : getHomes()) {
            final LazyLocation loc = homes.get(home);
            if (loc != null && world != null && loc.sameWorld(world)) {
                return loc;
            }
        }
        return homes.get(getHomes().get(0));
    }

    public List<String> getHomes() {
        return new ArrayList<>(homes.keySet());
    }

    public void setHome(String name, final LazyLocation loc) {
        name = StringUtil.safeString(name);
        homes.put(name, loc);
        config.save();
    }

    public void delHome(final String name) throws Exception {
        String search = getHomeName(name);
        if (!homes.containsKey(search)) {
            search = StringUtil.safeString(search);
        }
        if (homes.containsKey(search)) {
            homes.remove(search);
            config.save();
        } else {
            throw new TranslatableException("invalidHome", search);
        }
    }

    public void renameHome(final String name, final String newName) throws Exception {
        final LazyLocation location = homes.remove(name);
        if (location != null) {
            homes.put(StringUtil.safeString(newName), location);
            config.save();
        } else {
            throw new TranslatableException("invalidHome", name);
        }
    }

    public boolean hasHome() {
        return !homes.isEmpty();
    }

    public boolean hasHome(final String name) {
        return homes.containsKey(name);
    }

    // ---------------------------------------------------------------- misc fields

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nick) {
        nickname = nick;
        config.save();
    }

    public Set<String> getUnlimited() {
        return unlimited;
    }

    public boolean hasUnlimited(final ItemStack stack) {
        return unlimited.contains(itemKey(stack));
    }

    public void setUnlimited(final ItemStack stack, final boolean state) {
        final boolean wasUpdated;
        if (state) {
            wasUpdated = unlimited.add(itemKey(stack));
        } else {
            wasUpdated = unlimited.remove(itemKey(stack));
        }
        if (wasUpdated) {
            config.save();
        }
    }

    public void clearAllPowertools() {
        powertools.clear();
        config.save();
    }

    public List<String> getPowertool(final ItemStack stack) {
        return getPowertool(stack.getItem());
    }

    public List<String> getPowertool(final Item item) {
        return powertools.get(itemKey(item));
    }

    public void setPowertool(final ItemStack stack, final List<String> commandList) {
        if (commandList == null || commandList.isEmpty()) {
            powertools.remove(itemKey(stack));
        } else {
            powertools.put(itemKey(stack), commandList);
        }
        config.save();
    }

    public boolean hasPowerTools() {
        return !powertools.isEmpty();
    }

    public Map<String, List<String>> getAllPowertools() {
        return powertools;
    }

    public LazyLocation getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(final LazyLocation loc) {
        if (loc == null) {
            return;
        }
        lastLocation = loc;
        config.save();
    }

    public LazyLocation getLogoutLocation() {
        return logoutLocation;
    }

    public void setLogoutLocation(final LazyLocation loc) {
        if (loc == null) {
            return;
        }
        logoutLocation = loc;
        config.save();
    }

    public long getLastTeleportTimestamp() {
        return lastTeleport;
    }

    public void setLastTeleportTimestamp(final long time) {
        lastTeleport = time;
        config.save();
    }

    public long getLastHealTimestamp() {
        return lastHeal;
    }

    public void setLastHealTimestamp(final long time) {
        lastHeal = time;
        config.save();
    }

    public String getJail() {
        return jail;
    }

    public void setJail(final String jail) {
        this.jail = jail;
        config.save();
    }

    public int getMailAmount() {
        int amount = 0;
        for (final MailMessage element : mail) {
            if (!element.isExpired()) {
                amount++;
            }
        }
        return amount;
    }

    public int getUnreadMailAmount() {
        int unread = 0;
        for (final MailMessage element : mail) {
            if (!element.isRead() && !element.isExpired()) {
                unread++;
            }
        }
        return unread;
    }

    public List<MailMessage> getMailMessages() {
        return new ArrayList<>(mail);
    }

    public void setMailList(final List<MailMessage> messages) {
        mail.clear();
        mail.addAll(messages);
        config.save();
    }

    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public void setTeleportEnabled(final boolean set) {
        teleportEnabled = set;
        config.save();
    }

    public boolean isAutoTeleportEnabled() {
        return teleportAuto;
    }

    public void setAutoTeleportEnabled(final boolean set) {
        teleportAuto = set;
        config.save();
    }

    public void setIgnoredPlayerUUIDs(final List<UUID> players) {
        ignore.clear();
        ignore.addAll(players);
        config.save();
    }

    public boolean isIgnoredPlayer(final IUser user) {
        return ignore.contains(user.getUUID()) && !user.isIgnoreExempt();
    }

    public List<UUID> _getIgnoredPlayers() {
        return ignore;
    }

    public void setIgnoredPlayer(final IUser user, final boolean set) {
        final UUID id = user.getUUID();
        if (set) {
            if (!ignore.contains(id)) {
                ignore.add(id);
            }
        } else {
            ignore.remove(id);
        }
        config.save();
    }

    public boolean isGodModeEnabled() {
        return godMode;
    }

    public void setGodModeEnabled(final boolean set) {
        godMode = set;
        config.save();
    }

    public boolean isFlyModeEnabled() {
        return flyMode;
    }

    public void setFlyModeEnabled(final boolean set) {
        flyMode = set;
        config.save();
    }

    public boolean getMuted() {
        return muted;
    }

    public boolean isMuted() {
        return getMuted();
    }

    public void setMuted(final boolean set) {
        muted = set;
        config.save();
    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(final String reason) {
        muteReason = reason;
        config.save();
    }

    public boolean hasMuteReason() {
        return muteReason != null;
    }

    public long getMuteTimeout() {
        return muteTimeout;
    }

    public void setMuteTimeout(final long time) {
        muteTimeout = time;
        config.save();
    }

    public boolean isJailed() {
        return jailed;
    }

    public void setJailed(final boolean set) {
        jailed = set;
        config.save();
    }

    public boolean toggleJailed() {
        final boolean ret = !isJailed();
        setJailed(ret);
        return ret;
    }

    public long getJailTimeout() {
        return jailTimeout;
    }

    public void setJailTimeout(final long time) {
        jailTimeout = time;
        config.save();
    }

    public long getOnlineJailedTime() {
        return onlineJail;
    }

    public void setOnlineJailedTime(final long onlineJailed) {
        onlineJail = onlineJailed;
        config.save();
    }

    public long getLastLogin() {
        return login;
    }

    public void setLastLogin(final long time, final String address) {
        login = time;
        if (address != null) {
            ipAddress = address;
        }
        config.save();
    }

    public long getLastLogout() {
        return logout;
    }

    public void setLastLogout(final long time) {
        logout = time;
        config.save();
    }

    public String getLastLoginAddress() {
        return ipAddress;
    }

    public boolean isAfk() {
        return afk;
    }

    public void _setAfk(final boolean set) {
        afk = set;
        config.save();
    }

    public String getGeoLocation() {
        return geolocation;
    }

    public void setGeoLocation(final String geolocation) {
        this.geolocation = geolocation;
        config.save();
    }

    public boolean isSocialSpyEnabled() {
        return socialSpy;
    }

    public void setSocialSpyEnabled(final boolean status) {
        socialSpy = status;
        config.save();
    }

    public boolean isNPC() {
        return npc;
    }

    public void setNPC(final boolean set) {
        npc = set;
        config.save();
    }

    public String getLastAccountName() {
        return lastAccountName;
    }

    public void setLastAccountName(final String lastAccountName) {
        if (getLastAccountName() != null && !getLastAccountName().equals(lastAccountName)) {
            pastUsernames.add(0, getLastAccountName());
        }
        this.lastAccountName = lastAccountName;
        config.save();
    }

    public boolean arePowerToolsEnabled() {
        return powerToolsEnabled;
    }

    public void setPowerToolsEnabled(final boolean set) {
        powerToolsEnabled = set;
        config.save();
    }

    public boolean togglePowerToolsEnabled() {
        final boolean ret = !arePowerToolsEnabled();
        setPowerToolsEnabled(ret);
        return ret;
    }

    public long getKitTimestamp(String name) {
        name = name.replace('.', '_').replace('/', '_').toLowerCase(Locale.ENGLISH);
        final Long value = kitTimestamps.get(name);
        return value == null ? 0L : value;
    }

    public void setKitTimestamp(String name, final long time) {
        name = name.replace('.', '_').replace('/', '_').toLowerCase(Locale.ENGLISH);
        kitTimestamps.put(name, time);
        config.save();
    }

    public List<CommandCooldown> getCooldownsList() {
        return commandCooldowns;
    }

    public Map<Pattern, Long> getCommandCooldowns() {
        final Map<Pattern, Long> map = new HashMap<>();
        for (final CommandCooldown c : getCooldownsList()) {
            if (c == null) {
                continue;
            }
            map.put(c.pattern(), c.value());
        }
        return map;
    }

    public Date getCommandCooldownExpiry(final String label) {
        for (final CommandCooldown cooldown : getCooldownsList()) {
            if (cooldown.pattern().matcher(label).matches()) {
                return new Date(cooldown.value());
            }
        }
        return null;
    }

    public void addCommandCooldown(final Pattern pattern, final Date expiresAt, final boolean save) {
        commandCooldowns.add(new CommandCooldown(pattern, expiresAt.getTime()));
        if (save) {
            save();
        }
    }

    public boolean clearCommandCooldown(final Pattern pattern) {
        if (commandCooldowns.isEmpty()) {
            return false;
        }
        if (commandCooldowns.removeIf(cooldown -> cooldown != null && cooldown.pattern().equals(pattern))) {
            save();
            return true;
        }
        return false;
    }

    public boolean isAcceptingPay() {
        return acceptingPay;
    }

    public void setAcceptingPay(final boolean acceptingPay) {
        this.acceptingPay = acceptingPay;
        save();
    }

    public boolean isPromptingPayConfirm() {
        return confirmPay != null ? confirmPay : ess.getSettings().isConfirmCommandEnabledByDefault("pay");
    }

    public void setPromptingPayConfirm(final boolean prompt) {
        confirmPay = prompt;
        save();
    }

    public boolean isPromptingClearConfirm() {
        return confirmClear != null ? confirmClear : ess.getSettings().isConfirmCommandEnabledByDefault("clearinventory");
    }

    public void setPromptingClearConfirm(final boolean prompt) {
        confirmClear = prompt;
        save();
    }

    public boolean isLastMessageReplyRecipient() {
        return lastMessageReplyRecipient != null ? lastMessageReplyRecipient : ess.getSettings().isLastMessageReplyRecipient();
    }

    public void setLastMessageReplyRecipient(final boolean enabled) {
        lastMessageReplyRecipient = enabled;
        save();
    }

    public boolean isBaltopExcludeCache() {
        return baltopExempt;
    }

    public void setBaltopExemptCache(final boolean baltopExempt) {
        this.baltopExempt = baltopExempt;
        config.save();
    }

    public List<String> getPastUsernames() {
        return pastUsernames;
    }

    public void addPastUsername(final String username) {
        pastUsernames.add(0, username);
        config.save();
    }

    public boolean isShouting() {
        if (shouting == null) {
            shouting = ess.getSettings().isShoutDefault();
        }
        return shouting;
    }

    public void setShouting(final boolean shouting) {
        this.shouting = shouting;
        config.save();
    }

    public long getPlaytimeTicks() {
        return playtimeTicks;
    }

    public void addPlaytimeTicks(final long ticks) {
        playtimeTicks += ticks;
    }

    public void setPlaytimeTicks(final long ticks) {
        playtimeTicks = ticks;
        config.save();
    }

    public UUID getConfigUUID() {
        return uuid;
    }

    public void save() {
        config.save();
    }

    public void startTransaction() {
        config.startTransaction();
    }

    public void stopTransaction() {
        config.stopTransaction();
    }

    public void setConfigProperty(final String node, final Object object) {
        setConfigPropertyRaw("info." + node, object);
    }

    @SuppressWarnings("unchecked")
    public void setConfigPropertyRaw(final String node, final Object object) {
        if (node.startsWith("info.")) {
            final String[] parts = node.substring(5).split("\\.");
            Map<String, Object> current = info;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(parts[i], next);
                }
                current = (Map<String, Object>) next;
            }
            if (object == null) {
                current.remove(parts[parts.length - 1]);
            } else {
                current.put(parts[parts.length - 1], object);
            }
        } else {
            extensions.put(node, object);
        }
        config.save();
    }

    public Set<String> getConfigKeys() {
        return info.keySet();
    }

    public Map<String, Object> getConfigMap() {
        return info;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigMap(final String node) {
        final Object o = info.get(node);
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }
}
