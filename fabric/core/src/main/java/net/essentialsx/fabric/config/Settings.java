package net.essentialsx.fabric.config;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.signs.EssentialsSign;
import net.essentialsx.fabric.signs.Signs;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.item.Item;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Typed, reloadable view over {@code config.yml} (Section 6). Keys are identical to
 * EssentialsX so existing configuration can be reused.
 */
public class Settings {
    private static final BigDecimal DEFAULT_MAX_MONEY = new BigDecimal("10000000000000");
    private static final BigDecimal DEFAULT_MIN_MONEY = new BigDecimal("-10000000000000");
    private static final String DEFAULT_PRIMARY_COLOR = NamedTextColor.GOLD.toString();
    private static final String DEFAULT_SECONDARY_COLOR = NamedTextColor.RED.toString();

    public enum KeepInvPolicy {
        KEEP,
        DELETE,
        DROP
    }

    public enum TeleportWhenFreePolicy {
        SPAWN,
        BACK,
        OFF
    }

    private final transient YamlFile config;
    private final transient Essentials ess;
    private boolean teleportSafety;
    private boolean forceDisableTeleportSafety;
    private Set<String> disabledCommands = new HashSet<>();
    private List<String> overriddenCommands = Collections.emptyList();
    private List<String> playerCommands = Collections.emptyList();
    private Map<String, BigDecimal> commandCosts;
    private Set<String> socialSpyCommands = new HashSet<>();
    private Set<String> muteCommands = new HashSet<>();
    private String nicknamePrefix = "~";
    private String operatorColor = null;
    private List<Item> itemSpawnBl = new ArrayList<>();
    private List<EssentialsSign> enabledSigns = new ArrayList<>();
    private boolean signsEnabled = false;
    private boolean debug = false;
    private boolean configDebug = false;
    private boolean economyDisabled = false;
    private BigDecimal maxMoney = DEFAULT_MAX_MONEY;
    private BigDecimal minMoney = DEFAULT_MIN_MONEY;
    private boolean economyLog = false;
    private boolean economyLogUUID = false;
    private boolean economyLogUpdate = false;
    private boolean changeDisplayName = true;
    private boolean changePlayerListName = false;
    private boolean prefixsuffixconfigured = false;
    private boolean addprefixsuffix = false;
    private boolean disablePrefix = false;
    private boolean disableSuffix = false;
    private boolean getFreezeAfkPlayers;
    private boolean cancelAfkOnMove;
    private boolean cancelAfkOnInteract;
    private boolean sleepIgnoresAfkPlayers;
    private String afkListName;
    private boolean isAfkListName;
    private boolean broadcastAfkMessage;
    private KeepInvPolicy vanishingItemPolicy;
    private KeepInvPolicy bindingItemPolicy;
    private Set<String> noGodWorlds = new HashSet<>();
    private boolean registerBackInListener;
    private boolean disableItemPickupWhileAfk;
    private long teleportInvulnerabilityTime;
    private boolean teleportInvulnerability;
    private long loginAttackDelay;
    private int signUsePerSecond;
    private int mailsPerMinute;
    private long economyLagWarning;
    private long permissionsLagWarning;
    private boolean allowSilentJoin;
    private String customJoinMessage;
    private boolean isCustomJoinMessage;
    private String customQuitMessage;
    private boolean isCustomQuitMessage;
    private String customNewUsernameMessage;
    private boolean isCustomNewUsernameMessage;
    private List<String> spawnOnJoinGroups;
    private Map<Pattern, Long> commandCooldowns;
    private boolean npcsInBalanceRanking = false;
    private NumberFormat currencyFormat;
    private List<EssentialsSign> unprotectedSigns = Collections.emptyList();
    private List<String> defaultEnabledConfirmCommands;
    private TeleportWhenFreePolicy teleportWhenFreePolicy;
    private boolean isCompassTowardsHomePerm;
    private boolean isAllowWorldInBroadcastworld;
    private boolean allowOldIdSigns;
    private boolean isWaterSafe;
    private boolean isSafeUsermap;
    private boolean logCommandBlockCommands;
    private boolean logConsoleCommands;
    private Set<Predicate<String>> nickBlacklist;
    private boolean resetNickOnNameChange;
    private double maxProjectileSpeed;
    private boolean removeEffectsOnHeal;
    private Map<String, String> worldAliases;
    private String primaryColor = DEFAULT_PRIMARY_COLOR;
    private String secondaryColor = DEFAULT_SECONDARY_COLOR;
    private Set<String> multiplierPerms;
    private BigDecimal defaultMultiplier;
    private List<String> afkTimeoutCommands = Collections.emptyList();
    private String currencySymbol = "$";

    public Settings(final Essentials ess) {
        this.ess = ess;
        config = new YamlFile(ess.getDataFolder().resolve("config.yml"), "/config.yml");
        reloadConfig();
    }

    public Path getConfigFile() {
        return config.getFile();
    }

    public YamlFile getConfig() {
        return config;
    }

    // ------------------------------------------------------------ spawn module keys

    public boolean getRespawnAtHome() {
        return config.getBoolean("respawn-at-home", false);
    }

    public String getRandomSpawnLocation() {
        return config.getString("random-spawn-location", "none");
    }

    public String getRandomRespawnLocation() {
        return config.getString("random-respawn-location", "none");
    }

    public boolean isRespawnAtAnchor() {
        return config.getBoolean("respawn-at-anchor", false);
    }

    public boolean getUpdateBedAtDaytime() {
        return config.getBoolean("update-bed-at-daytime", true);
    }

    public boolean isRespawnAtBed() {
        return config.getBoolean("respawn-at-home-bed", true);
    }

    public boolean getAnnounceNewPlayers() {
        return !config.getString("newbies.announce-format", "-").isEmpty();
    }

    public String getAnnounceNewPlayerFormat() {
        return FormatUtil.replaceFormat(config.getString("newbies.announce-format", "&dWelcome {DISPLAYNAME} to the server!"));
    }

    public String getNewPlayerKit() {
        return config.getString("newbies.kit", "");
    }

    public String getNewbieSpawn() {
        return config.getString("newbies.spawnpoint", "default");
    }

    public String getRespawnPriority() {
        return config.getString("respawn-listener-priority", "normal").toLowerCase(Locale.ENGLISH);
    }

    public String getSpawnJoinPriority() {
        return config.getString("spawn-join-listener-priority", "normal").toLowerCase(Locale.ENGLISH);
    }

    public boolean isSpawnOnJoin() {
        return !this.spawnOnJoinGroups.isEmpty();
    }

    private List<String> _getSpawnOnJoinGroups() {
        final List<String> def = Collections.emptyList();
        if (config.hasProperty("spawn-on-join")) {
            if (config.isList("spawn-on-join")) {
                return new ArrayList<>(config.getStringList("spawn-on-join"));
            } else if (config.isBoolean("spawn-on-join")) {
                return config.getBoolean("spawn-on-join", true) ? Collections.singletonList("*") : def;
            }
            final String val = config.get("spawn-on-join").toString();
            return !val.isEmpty() ? Collections.singletonList(val) : def;
        } else {
            return def;
        }
    }

    public List<String> getSpawnOnJoinGroups() {
        return this.spawnOnJoinGroups;
    }

    public boolean isUserInSpawnOnJoinGroup(final User user) {
        for (final String group : this.spawnOnJoinGroups) {
            if (group.equals("*") || user.inGroup(group)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ homes

    public Set<String> getMultipleHomes() {
        return config.isSection("sethome-multiple") ? config.getKeys("sethome-multiple") : null;
    }

    public int getHomeLimit(final User user) {
        int limit = 1;
        if (user.isAuthorized("essentials.sethome.multiple")) {
            limit = getHomeLimit("default");
        }
        final Set<String> homeList = getMultipleHomes();
        if (homeList != null) {
            for (final String set : homeList) {
                if (user.isAuthorized("essentials.sethome.multiple." + set) && limit < getHomeLimit(set)) {
                    limit = getHomeLimit(set);
                }
            }
        }
        return limit;
    }

    public int getHomeLimit(final String set) {
        return config.getInt("sethome-multiple." + set, config.getInt("sethome-multiple.default", 3));
    }

    public int getNearRadius() {
        return config.getInt("near-radius", 200);
    }

    public long getChatRadius() {
        return config.getLong("chat.radius", 0);
    }

    public boolean isShoutDefault() {
        return config.getBoolean("chat.shout-default", false);
    }

    public boolean isPersistShout() {
        return config.getBoolean("chat.persist-shout", false);
    }

    // ------------------------------------------------------------ teleport

    public boolean isTeleportSafetyEnabled() {
        return teleportSafety;
    }

    public boolean isForceDisableTeleportSafety() {
        return forceDisableTeleportSafety;
    }

    public boolean isAlwaysTeleportSafety() {
        return config.getBoolean("force-safe-teleport-location", false);
    }

    public boolean isConsiderWorldHeightForTeleportSafety() {
        return config.getBoolean("consider-world-height-for-teleport-safety", false);
    }

    public boolean isTeleportPassengerDismount() {
        return config.getBoolean("teleport-passenger-dismount", true);
    }

    public boolean isForcePassengerTeleport() {
        return config.getBoolean("force-passenger-teleportation", false);
    }

    public double getTeleportDelay() {
        return config.getDouble("teleport-delay", 0);
    }

    public double getTeleportCooldown() {
        return config.getDouble("teleport-cooldown", 0);
    }

    public boolean isTeleportToCenterLocation() {
        return config.getBoolean("teleport-to-center", true);
    }

    public long getTpaAcceptCancellation() {
        return config.getLong("tpa-accept-cancellation", 120);
    }

    public int getTpaMaxRequests() {
        return config.getInt("tpa-max-requests", 5);
    }

    public long getTeleportInvulnerability() {
        return teleportInvulnerabilityTime;
    }

    public boolean isTeleportInvulnerability() {
        return teleportInvulnerability;
    }

    public boolean isWorldTeleportPermissions() {
        return config.getBoolean("world-teleport-permissions", false);
    }

    public boolean isWorldHomePermissions() {
        return config.getBoolean("world-home-permissions", false);
    }

    public boolean registerBackInListener() {
        return registerBackInListener;
    }

    public boolean isSpawnIfNoHome() {
        return config.getBoolean("spawn-if-no-home", true);
    }

    public boolean isConfirmHomeOverwrite() {
        return config.getBoolean("confirm-home-overwrite", false);
    }

    public boolean isWaterSafe() {
        return isWaterSafe;
    }

    // ------------------------------------------------------------ items / kits

    public int getOversizedStackSize() {
        return config.getInt("oversized-stacksize", 64);
    }

    public int getDefaultStackSize() {
        return config.getInt("default-stack-size", -1);
    }

    public boolean isKitAutoEquip() {
        return config.getBoolean("kit-auto-equip", false);
    }

    public boolean isPastebinCreateKit() {
        return config.getBoolean("pastebin-createkit", false);
    }

    public boolean isUseBetterKits() {
        return config.getBoolean("use-nbt-serialization-in-createkit", false);
    }

    public boolean isSkippingUsedOneTimeKitsFromKitList() {
        return config.getBoolean("skip-used-one-time-kits-from-kit-list", false);
    }

    public boolean isDropItemsIfFull() {
        return config.getBoolean("drop-items-if-full", false);
    }

    public boolean allowUnsafeEnchantments() {
        return config.getBoolean("unsafe-enchantments", false);
    }

    public boolean getRepairEnchanted() {
        return config.getBoolean("repair-enchanted", true);
    }

    public boolean permissionBasedItemSpawn() {
        return config.getBoolean("permission-based-item-spawn", false);
    }

    public List<Item> itemSpawnBlacklist() {
        return itemSpawnBl;
    }

    public void _lateLoadItemSpawnBlacklist() {
        itemSpawnBl = _getItemSpawnBlacklist();
    }

    private List<Item> _getItemSpawnBlacklist() {
        final List<Item> epItemSpwn = new ArrayList<>();
        if (ess.getItemDb() == null || !ess.getItemDb().isReady()) {
            return epItemSpwn;
        }
        for (String itemName : config.getString("item-spawn-blacklist", "").split(",")) {
            itemName = itemName.trim();
            if (itemName.isEmpty()) {
                continue;
            }
            try {
                epItemSpwn.add(ess.getItemDb().get(itemName).getItem());
            } catch (final Exception ex) {
                ess.getLogger().error(Text.get().miniToLegacy(tlLiteral("unknownItemInList", itemName, "item-spawn-blacklist")));
            }
        }
        return epItemSpwn;
    }

    public boolean isTradeInStacks(final Item type) {
        return config.getBoolean("trade-in-stacks." + net.essentialsx.fabric.user.UserData.itemKey(type).replace("_", ""), false);
    }

    public boolean isAllowBulkBuySell() {
        return config.getBoolean("allow-bulk-buy-sell", false);
    }

    public boolean isAllowSellNamedItems() {
        return config.getBoolean("allow-selling-named-items", false);
    }

    public int getMaxItemLore() {
        return config.getInt("max-itemlore-lines", 10);
    }

    public boolean isDirectHatAllowed() {
        return config.getBoolean("allow-direct-hat", true);
    }

    public int getSpawnMobLimit() {
        return config.getInt("spawnmob-limit", 10);
    }

    public int getMaxTreeCommandRange() {
        return config.getInt("tree-command-range-limit", 300);
    }

    public double getMaxProjectileSpeed() {
        return maxProjectileSpeed;
    }

    public boolean isMilkBucketEasterEggEnabled() {
        return config.getBoolean("milk-bucket-easter-egg", true);
    }

    // ------------------------------------------------------------ commands

    public boolean isCommandDisabled(final String label) {
        return disabledCommands.contains(label);
    }

    public Set<String> getDisabledCommands() {
        return disabledCommands;
    }

    public boolean isVerboseCommandUsages() {
        return config.getBoolean("verbose-command-usages", true);
    }

    private Set<String> _getDisabledCommands() {
        final Set<String> disCommands = new HashSet<>();
        for (final String c : config.getStringList("disabled-commands")) {
            disCommands.add(c.toLowerCase(Locale.ENGLISH));
        }
        for (final String c : config.getKeys()) {
            if (c.startsWith("disable-")) {
                disCommands.add(c.substring(8).toLowerCase(Locale.ENGLISH));
            }
        }
        return disCommands;
    }

    public boolean isPlayerCommand(final String label) {
        for (final String c : playerCommands) {
            if (c.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public boolean isCommandOverridden(final String name) {
        for (final String c : overriddenCommands) {
            if (c.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return config.getBoolean("override-" + name.toLowerCase(Locale.ENGLISH), false);
    }

    private Map<String, BigDecimal> _getCommandCosts() {
        final Map<String, Object> section = config.getSection("command-costs");
        if (section != null && !section.isEmpty()) {
            final Map<String, BigDecimal> newMap = new HashMap<>();
            for (final Map.Entry<String, Object> entry : section.entrySet()) {
                final String command = entry.getKey();
                final Object node = entry.getValue();
                if (command.charAt(0) == '/') {
                    ess.getLogger().warn("Invalid command cost. '" + command + "' should not start with '/'.");
                }
                try {
                    if (node instanceof Number) {
                        newMap.put(command.toLowerCase(Locale.ENGLISH), new BigDecimal(node.toString()));
                    } else if (node instanceof String) {
                        final double cost = Double.parseDouble(((String) node).trim().replace("$", "").replace(getCurrencySymbol(), "").replaceAll("\\W", ""));
                        newMap.put(command.toLowerCase(Locale.ENGLISH), BigDecimal.valueOf(cost));
                    } else {
                        ess.getLogger().warn("Invalid command cost for: " + command);
                    }
                } catch (final Exception ex) {
                    ess.getLogger().warn("Invalid command cost for: " + command);
                }
            }
            return newMap;
        }
        return null;
    }

    public BigDecimal getCommandCost(String name) {
        name = name.replace('.', '_').replace('/', '_');
        if (commandCosts != null && commandCosts.containsKey(name)) {
            return commandCosts.get(name);
        }
        return BigDecimal.ZERO;
    }

    private Set<String> _getSocialSpyCommands() {
        final Set<String> socialspyCommands = new HashSet<>();
        if (config.isList("socialspy-commands")) {
            for (final String c : config.getStringList("socialspy-commands")) {
                socialspyCommands.add(c.toLowerCase(Locale.ENGLISH));
            }
        } else {
            socialspyCommands.addAll(Arrays.asList("msg", "r", "mail", "m", "whisper", "emsg", "t", "tell", "er", "reply", "ereply", "email", "action", "describe", "eme", "eaction", "edescribe", "etell", "ewhisper", "pm"));
        }
        return socialspyCommands;
    }

    public Set<String> getSocialSpyCommands() {
        return socialSpyCommands;
    }

    public boolean getSocialSpyListenMutedPlayers() {
        return config.getBoolean("socialspy-listen-muted-players", true);
    }

    public boolean isSocialSpyMessages() {
        return config.getBoolean("socialspy-messages", true);
    }

    public boolean isSocialSpyDisplayNames() {
        return config.getBoolean("socialspy-uses-displaynames", true);
    }

    private Set<String> _getMuteCommands() {
        final Set<String> muteCommands = new HashSet<>();
        if (config.isList("mute-commands")) {
            for (final String s : config.getStringList("mute-commands")) {
                muteCommands.add(s.toLowerCase(Locale.ENGLISH));
            }
        }
        return muteCommands;
    }

    public Set<String> getMuteCommands() {
        return muteCommands;
    }

    private Map<Pattern, Long> _getCommandCooldowns() {
        final Map<String, Object> section = config.getSection("command-cooldowns");
        if (section == null) {
            return null;
        }
        final Map<Pattern, Long> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : section.entrySet()) {
            String cmdEntry = entry.getKey();
            Object value = entry.getValue();
            Pattern pattern = null;
            if (cmdEntry.startsWith("^")) {
                try {
                    pattern = Pattern.compile(cmdEntry.substring(1));
                } catch (final PatternSyntaxException e) {
                    ess.getLogger().warn("Command cooldown error: " + e.getMessage());
                }
            } else {
                if (cmdEntry.startsWith("\\^")) {
                    cmdEntry = cmdEntry.substring(1);
                }
                final String cmd = cmdEntry.replaceAll("\\*", ".*");
                pattern = Pattern.compile(cmd + "( .*)?");
            }
            if (value instanceof String) {
                try {
                    value = Double.parseDouble(value.toString());
                } catch (final NumberFormatException ignored) {
                }
            }
            if (!(value instanceof Number)) {
                ess.getLogger().warn("Command cooldown error: '" + value + "' is not a valid cooldown");
                continue;
            }
            final double cooldown = ((Number) value).doubleValue();
            if (cooldown < 1) {
                ess.getLogger().warn("Command cooldown with very short " + cooldown + " cooldown.");
            }
            if (pattern != null) {
                result.put(pattern, (long) cooldown * 1000);
            }
        }
        return result;
    }

    public boolean isCommandCooldownsEnabled() {
        return commandCooldowns != null;
    }

    public long getCommandCooldownMs(final String label) {
        final Map.Entry<Pattern, Long> result = getCommandCooldownEntry(label);
        return result != null ? result.getValue() : -1;
    }

    public Map.Entry<Pattern, Long> getCommandCooldownEntry(final String label) {
        if (isCommandCooldownsEnabled()) {
            for (final Map.Entry<Pattern, Long> entry : this.commandCooldowns.entrySet()) {
                final boolean matches = entry.getKey().matcher(label).matches();
                if (isDebug()) {
                    ess.getLogger().info(String.format("Checking command '%s' against cooldown '%s': %s", label, entry.getKey(), matches));
                }
                if (matches) {
                    return entry;
                }
            }
        }
        return null;
    }

    public boolean isCommandCooldownPersistent(final String label) {
        return config.getBoolean("command-cooldown-persistence", true);
    }

    private List<String> _getDefaultEnabledConfirmCommands() {
        final List<String> commands = config.getStringList("default-enabled-confirm-commands");
        commands.replaceAll(String::toLowerCase);
        return commands;
    }

    public List<String> getDefaultEnabledConfirmCommands() {
        return defaultEnabledConfirmCommands;
    }

    public boolean isConfirmCommandEnabledByDefault(final String commandName) {
        return getDefaultEnabledConfirmCommands().contains(commandName.toLowerCase());
    }

    public boolean showNonEssCommandsInHelp() {
        return config.getBoolean("non-ess-in-help", true);
    }

    public boolean hidePermissionlessHelp() {
        return config.getBoolean("hide-permissionless-help", true);
    }

    public boolean logCommandBlockCommands() {
        return logCommandBlockCommands;
    }

    public boolean logConsoleCommands() {
        return logConsoleCommands;
    }

    // ------------------------------------------------------------ nick / display

    public String getNicknamePrefix() {
        return nicknamePrefix;
    }

    public boolean isResetNickOnNameChange() {
        return resetNickOnNameChange;
    }

    public String getOperatorColor() {
        return operatorColor;
    }

    private String _getOperatorColor() {
        final String colorName = config.getString("ops-name-color", null);
        if (colorName == null) {
            return ChatColor.RED.toString();
        } else if (colorName.equalsIgnoreCase("none") || colorName.isEmpty()) {
            return null;
        }
        try {
            return FormatUtil.parseHexColor(colorName);
        } catch (final NumberFormatException ignored) {
        }
        final ChatColor named = ChatColor.valueOfSafe(colorName);
        if (named != null) {
            return named.toString();
        }
        final ChatColor lastResort = ChatColor.getByChar(colorName);
        if (lastResort != null) {
            return lastResort.toString();
        }
        return null;
    }

    public boolean changeDisplayName() {
        return changeDisplayName;
    }

    public boolean changePlayerListName() {
        return changePlayerListName;
    }

    public boolean changeTabCompleteName() {
        return config.getBoolean("change-tab-complete-name", false);
    }

    public boolean addPrefixSuffix() {
        return prefixsuffixconfigured ? addprefixsuffix : false;
    }

    public boolean disablePrefix() {
        return disablePrefix;
    }

    public boolean disableSuffix() {
        return disableSuffix;
    }

    public boolean isAddingPrefixInPlayerlist() {
        return config.getBoolean("add-prefix-in-playerlist", false);
    }

    public boolean isAddingSuffixInPlayerlist() {
        return config.getBoolean("add-suffix-in-playerlist", false);
    }

    public int getMaxNickLength() {
        return config.getInt("max-nick-length", 30);
    }

    public boolean ignoreColorsInMaxLength() {
        return config.getBoolean("ignore-colors-in-max-nick-length", false);
    }

    public boolean hideDisplayNameInVanish() {
        return config.getBoolean("hide-displayname-in-vanish", false);
    }

    public String getNickRegex() {
        return config.getString("allowed-nicks-regex", "^[a-zA-Z_0-9§]+$");
    }

    public Set<Predicate<String>> getNickBlacklist() {
        return nickBlacklist;
    }

    private Set<Predicate<String>> _getNickBlacklist() {
        final Set<Predicate<String>> blacklist = new HashSet<>();
        config.getStringList("nick-blacklist").forEach(entry -> {
            try {
                blacklist.add(Pattern.compile(entry).asPredicate());
            } catch (final PatternSyntaxException e) {
                ess.getLogger().warn("Invalid nickname blacklist regex: " + entry);
            }
        });
        return blacklist;
    }

    public boolean realNamesOnList() {
        return config.getBoolean("real-names-on-list", false);
    }

    public Map<String, Object> getListGroupConfig() {
        final Map<String, Object> node = config.getSection("list");
        if (node != null && !node.isEmpty()) {
            return node;
        }
        final Map<String, Object> defaultMap = new HashMap<>();
        if (config.getBoolean("sort-list-by-groups", false)) {
            defaultMap.put("ListByGroup", "ListByGroup");
        } else {
            defaultMap.put("Players", "*");
        }
        return defaultMap;
    }

    public String getWorldAlias(final String world) {
        return worldAliases.getOrDefault(world.toLowerCase(), world);
    }

    private Map<String, String> _getWorldAliases() {
        final Map<String, String> map = new HashMap<>();
        final Map<String, Object> section = config.getSection("chat.world-aliases");
        if (section == null) {
            return map;
        }
        for (final Map.Entry<String, Object> entry : section.entrySet()) {
            map.put(entry.getKey().toLowerCase(), FormatUtil.replaceFormat(String.valueOf(entry.getValue())));
        }
        return map;
    }

    /** Fabric addition: map legacy Bukkit world names to dimension keys. */
    private Map<String, String> _getDimensionAliases() {
        final Map<String, String> map = new HashMap<>();
        final Map<String, Object> section = config.getSection("dimension-aliases");
        if (section == null) {
            return map;
        }
        for (final Map.Entry<String, Object> entry : section.entrySet()) {
            map.put(entry.getKey().toLowerCase(Locale.ENGLISH), String.valueOf(entry.getValue()));
        }
        return map;
    }

    // ------------------------------------------------------------ reload

    public void reloadConfig() {
        config.load();
        noGodWorlds = new HashSet<>(config.getStringList("no-god-in-worlds"));
        enabledSigns = _getEnabledSigns();
        teleportSafety = config.getBoolean("teleport-safety", true);
        forceDisableTeleportSafety = config.getBoolean("force-disable-teleport-safety", false);
        teleportInvulnerabilityTime = config.getLong("teleport-invulnerability", 0) * 1000;
        teleportInvulnerability = config.getLong("teleport-invulnerability", 0) > 0;
        disableItemPickupWhileAfk = config.getBoolean("disable-item-pickup-while-afk", false);
        registerBackInListener = config.getBoolean("register-back-in-listener", false);
        cancelAfkOnInteract = config.getBoolean("cancel-afk-on-interact", true);
        cancelAfkOnMove = config.getBoolean("cancel-afk-on-move", true);
        getFreezeAfkPlayers = config.getBoolean("freeze-afk-players", false);
        sleepIgnoresAfkPlayers = config.getBoolean("sleep-ignores-afk-players", true);
        afkListName = FormatUtil.replaceFormat(config.getString("afk-list-name", "none"));
        isAfkListName = afkListName != null && !afkListName.equalsIgnoreCase("none");
        broadcastAfkMessage = config.getBoolean("broadcast-afk-message", true);
        itemSpawnBl = _getItemSpawnBlacklist();
        loginAttackDelay = config.getLong("login-attack-delay", 0) * 1000;
        final int perSec = config.getInt("sign-use-per-second", 4);
        signUsePerSecond = perSec > 0 ? perSec : 1;
        changeDisplayName = config.getBoolean("change-displayname", true);
        disabledCommands = _getDisabledCommands();
        overriddenCommands = config.getStringList("overridden-commands");
        playerCommands = config.getStringList("player-commands");
        nicknamePrefix = config.getString("nickname-prefix", "~");
        resetNickOnNameChange = config.getBoolean("reset-nick-on-name-change", false);
        operatorColor = _getOperatorColor();
        changePlayerListName = config.getBoolean("change-playerlist", false);
        configDebug = config.getBoolean("debug", false);
        prefixsuffixconfigured = config.hasProperty("add-prefix-suffix");
        addprefixsuffix = config.getBoolean("add-prefix-suffix", false);
        disablePrefix = config.getBoolean("disablePrefix", false);
        disableSuffix = config.getBoolean("disableSuffix", false);
        commandCosts = _getCommandCosts();
        socialSpyCommands = _getSocialSpyCommands();
        mailsPerMinute = config.getInt("mails-per-minute", 1000);
        maxMoney = config.getBigDecimal("max-money", DEFAULT_MAX_MONEY);
        BigDecimal min = config.getBigDecimal("min-money", DEFAULT_MIN_MONEY);
        if (min.signum() > 0) {
            min = min.negate();
        }
        minMoney = min;
        permissionsLagWarning = (long) (config.getDouble("permissions-lag-warning", 25.0) * 1000000);
        economyLagWarning = (long) (config.getDouble("economy-lag-warning", 25.0) * 1000000);
        economyLog = config.getBoolean("economy-log-enabled", false);
        economyLogUUID = config.getBoolean("economy-log-uuids", false);
        economyLogUpdate = config.getBoolean("economy-log-update-enabled", false);
        economyDisabled = config.getBoolean("disable-eco", false);
        allowSilentJoin = config.getBoolean("allow-silent-join-quit", false);
        customJoinMessage = FormatUtil.replaceFormat(config.getString("custom-join-message", "none"));
        isCustomJoinMessage = !customJoinMessage.equals("none");
        customQuitMessage = FormatUtil.replaceFormat(config.getString("custom-quit-message", "none"));
        isCustomQuitMessage = !customQuitMessage.equals("none");
        customNewUsernameMessage = FormatUtil.replaceFormat(config.getString("custom-new-username-message", "none"));
        isCustomNewUsernameMessage = !customNewUsernameMessage.equals("none");
        muteCommands = _getMuteCommands();
        spawnOnJoinGroups = _getSpawnOnJoinGroups();
        commandCooldowns = _getCommandCooldowns();
        npcsInBalanceRanking = config.getBoolean("npcs-in-balance-ranking", false);
        currencyFormat = _getCurrencyFormat();
        unprotectedSigns = _getUnprotectedSign();
        defaultEnabledConfirmCommands = _getDefaultEnabledConfirmCommands();
        teleportWhenFreePolicy = _getTeleportWhenFreePolicy();
        isCompassTowardsHomePerm = config.getBoolean("compass-towards-home-perm", false);
        isAllowWorldInBroadcastworld = config.getBoolean("allow-world-in-broadcastworld", false);
        allowOldIdSigns = config.getBoolean("allow-old-id-signs", false);
        isWaterSafe = config.getBoolean("is-water-safe", false);
        LocationUtil.setIsWaterSafe(isWaterSafe);
        isSafeUsermap = config.getBoolean("safe-usermap-names", true);
        logCommandBlockCommands = config.getBoolean("log-command-block-commands", true);
        logConsoleCommands = config.getBoolean("log-console-commands", true);
        nickBlacklist = _getNickBlacklist();
        maxProjectileSpeed = config.getDouble("max-projectile-speed", 8);
        removeEffectsOnHeal = config.getBoolean("remove-effects-on-heal", true);
        vanishingItemPolicy = _getKeepInvPolicy("vanishing-items-policy");
        bindingItemPolicy = _getKeepInvPolicy("binding-items-policy");
        currencySymbol = _getCurrencySymbol();
        worldAliases = _getWorldAliases();
        Worlds.setConfiguredAliases(_getDimensionAliases());
        primaryColor = _getPrimaryColor();
        secondaryColor = _getSecondaryColor();
        multiplierPerms = config.isSection("sell-multipliers") ? config.getKeys("sell-multipliers") : null;
        defaultMultiplier = config.getBigDecimal("sell-multipliers.default", BigDecimal.ONE);
        afkTimeoutCommands = new ArrayList<>(config.getStringList("afk-timeout-commands"));
    }

    // ------------------------------------------------------------ signs

    public List<EssentialsSign> enabledSigns() {
        return enabledSigns;
    }

    public boolean areSignsDisabled() {
        return !signsEnabled;
    }

    private List<EssentialsSign> _getEnabledSigns() {
        this.signsEnabled = false;
        final List<EssentialsSign> newSigns = new ArrayList<>();
        for (String signName : config.getStringList("enabledSigns")) {
            signName = signName.trim().toUpperCase(Locale.ENGLISH);
            if (signName.isEmpty()) {
                continue;
            }
            if (signName.equals("COLOR") || signName.equals("COLOUR")) {
                signsEnabled = true;
                continue;
            }
            try {
                newSigns.add(Signs.valueOf(signName).getSign());
            } catch (final Exception ex) {
                ess.getLogger().error(Text.get().miniToLegacy(tlLiteral("unknownItemInList", signName, "enabledSigns")));
                continue;
            }
            signsEnabled = true;
        }
        return newSigns;
    }

    public List<EssentialsSign> getUnprotectedSignNames() {
        return this.unprotectedSigns;
    }

    private List<EssentialsSign> _getUnprotectedSign() {
        final List<EssentialsSign> newSigns = new ArrayList<>();
        for (String signName : config.getStringList("unprotected-sign-names")) {
            signName = signName.trim().toUpperCase(Locale.ENGLISH);
            if (signName.isEmpty()) {
                continue;
            }
            try {
                newSigns.add(Signs.valueOf(signName).getSign());
            } catch (final Exception ex) {
                ess.getLogger().error(Text.get().miniToLegacy(tlLiteral("unknownItemInList", signName, "unprotected-sign-names")));
            }
        }
        return newSigns;
    }

    public int getSignUsePerSecond() {
        return signUsePerSecond;
    }

    public boolean allowOldIdSigns() {
        return allowOldIdSigns;
    }

    // ------------------------------------------------------------ debug / misc

    public boolean isDebug() {
        return debug || configDebug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public boolean warnOnSmite() {
        return config.getBoolean("warn-on-smite", true);
    }

    public String getLocale() {
        return config.getString("locale", "");
    }

    public boolean isPerPlayerLocale() {
        return config.getBoolean("per-player-locale", false);
    }

    public long getBackupInterval() {
        return config.getInt("backup.interval", 1440);
    }

    public String getBackupCommand() {
        return config.getString("backup.command", null);
    }

    public boolean isAlwaysRunBackup() {
        return config.getBoolean("backup.always-run", false);
    }

    public boolean isUpdateCheckEnabled() {
        return config.getBoolean("update-check", false);
    }

    public boolean isSafeUsermap() {
        return isSafeUsermap;
    }

    public int getMaxUserCacheCount() {
        final long count = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        return config.getInt("max-user-cache-count", (int) count);
    }

    public long getMaxUserCacheValueExpiry() {
        return config.getLong("max-user-cache-value-expiry", 600);
    }

    // ------------------------------------------------------------ economy

    private String _getCurrencySymbol() {
        String value = config.getString("currency-symbol", "$").trim();
        if (value.length() > 1 || value.matches("\\d")) {
            value = "$";
        }
        return value;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public boolean isCurrencySymbolSuffixed() {
        return config.getBoolean("currency-symbol-suffix", false);
    }

    public boolean isEcoDisabled() {
        return economyDisabled;
    }

    public BigDecimal getStartingBalance() {
        return config.getBigDecimal("starting-balance", BigDecimal.ZERO);
    }

    public BigDecimal getMaxMoney() {
        return maxMoney;
    }

    public BigDecimal getMinMoney() {
        return minMoney;
    }

    public boolean isEcoLogEnabled() {
        return economyLog;
    }

    public boolean isEcoLogUUIDEnabled() {
        return economyLogUUID;
    }

    public boolean isEcoLogUpdateEnabled() {
        return economyLogUpdate;
    }

    public long getEconomyLagWarning() {
        return economyLagWarning;
    }

    public long getPermissionsLagWarning() {
        return permissionsLagWarning;
    }

    public BigDecimal getMinimumPayAmount() {
        return new BigDecimal(config.getString("minimum-pay-amount", "0.001"));
    }

    public boolean isPayExcludesIgnoreList() {
        return config.getBoolean("pay-excludes-ignore-list", false);
    }

    public boolean isNpcsInBalanceRanking() {
        return npcsInBalanceRanking;
    }

    private NumberFormat _getCurrencyFormat() {
        final String currencyFormatString = config.getString("currency-format", "#,##0.00");
        final String symbolLocaleString = config.getString("currency-symbol-format-locale", null);
        final DecimalFormatSymbols decimalFormatSymbols;
        if (symbolLocaleString != null) {
            decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag(symbolLocaleString));
        } else {
            decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.US);
        }
        final DecimalFormat currencyFormat = new DecimalFormat(currencyFormatString, decimalFormatSymbols);
        currencyFormat.setRoundingMode(RoundingMode.FLOOR);
        NumberUtil.internalSetPrettyFormat(currencyFormat);
        return currencyFormat;
    }

    public NumberFormat getCurrencyFormat() {
        return this.currencyFormat;
    }

    public boolean showZeroBaltop() {
        return config.getBoolean("show-zero-baltop", true);
    }

    public BigDecimal getBaltopMinBalance() {
        return config.getBigDecimal("baltop-requirements.minimum-balance", BigDecimal.ZERO);
    }

    public int getBaltopEntryLimit() {
        return config.getInt("baltop-entry-limit", -1);
    }

    public long getBaltopMinPlaytime() {
        return config.getLong("baltop-requirements.minimum-playtime", 0);
    }

    public BigDecimal getMultiplier(final User user) {
        BigDecimal multiplier = defaultMultiplier;
        if (multiplierPerms == null) {
            return defaultMultiplier;
        }
        for (final String multiplierPerm : multiplierPerms) {
            if (user.isAuthorized("essentials.sell.multiplier." + multiplierPerm)) {
                final BigDecimal value = config.getBigDecimal("sell-multipliers." + multiplierPerm, BigDecimal.ZERO);
                if (value.compareTo(multiplier) > 0) {
                    multiplier = value;
                }
            }
        }
        return multiplier;
    }

    /** Fabric addition (Section 10.2): Impactor currency identifier or "primary". */
    public String getImpactorCurrency() {
        return config.getString("economy.impactor-currency", "primary");
    }

    /** Fabric addition (Section 10.2): behaviour of /eco reset. */
    public boolean isEcoResetUsesStartingBalance() {
        return config.getBoolean("economy.reset-to-essentials-starting-balance", false);
    }

    // ------------------------------------------------------------ afk / activity

    public long getAutoAfk() {
        return config.getLong("auto-afk", 300);
    }

    public long getAutoAfkTimeout() {
        return config.getLong("auto-afk-timeout", config.getLong("auto-afk-kick", -1));
    }

    public List<String> getAfkTimeoutCommands() {
        return afkTimeoutCommands;
    }

    public boolean getFreezeAfkPlayers() {
        return getFreezeAfkPlayers;
    }

    public boolean cancelAfkOnMove() {
        return cancelAfkOnMove;
    }

    public boolean cancelAfkOnInteract() {
        return cancelAfkOnInteract;
    }

    public boolean cancelAfkOnChat() {
        return config.getBoolean("cancel-afk-on-chat", true);
    }

    public boolean cancelAfkOnFish() {
        return config.getBoolean("cancel-afk-on-fish", true);
    }

    public boolean sleepIgnoresAfkPlayers() {
        return sleepIgnoresAfkPlayers;
    }

    public boolean sleepIgnoresVanishedPlayers() {
        return config.getBoolean("sleep-ignores-vanished-player", true);
    }

    public boolean isAfkListName() {
        return isAfkListName;
    }

    public String getAfkListName() {
        return afkListName;
    }

    public boolean broadcastAfkMessage() {
        return broadcastAfkMessage;
    }

    public boolean getDisableItemPickupWhileAfk() {
        return disableItemPickupWhileAfk;
    }

    // ------------------------------------------------------------ death / god

    public boolean areDeathMessagesEnabled() {
        return config.getBoolean("death-messages", true);
    }

    private KeepInvPolicy _getKeepInvPolicy(final String key) {
        final String value = config.getString(key, "keep").toLowerCase(Locale.ENGLISH);
        try {
            return KeepInvPolicy.valueOf(value.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException e) {
            return KeepInvPolicy.KEEP;
        }
    }

    public KeepInvPolicy getVanishingItemsPolicy() {
        return vanishingItemPolicy;
    }

    public KeepInvPolicy getBindingItemsPolicy() {
        return bindingItemPolicy;
    }

    public Set<String> getNoGodWorlds() {
        return noGodWorlds;
    }

    public boolean removeGodOnDisconnect() {
        return config.getBoolean("remove-god-on-disconnect", false);
    }

    public boolean infoAfterDeath() {
        return config.getBoolean("send-info-after-death", false);
    }

    public double getHealCooldown() {
        return config.getDouble("heal-cooldown", 0);
    }

    public boolean isRemovingEffectsOnHeal() {
        return removeEffectsOnHeal;
    }

    public long getLoginAttackDelay() {
        return loginAttackDelay;
    }

    // ------------------------------------------------------------ join / quit

    public boolean allowSilentJoinQuit() {
        return allowSilentJoin;
    }

    public String getCustomJoinMessage() {
        return customJoinMessage;
    }

    public boolean isCustomJoinMessage() {
        return isCustomJoinMessage;
    }

    public String getCustomQuitMessage() {
        return customQuitMessage;
    }

    public boolean isCustomQuitMessage() {
        return isCustomQuitMessage;
    }

    public String getCustomNewUsernameMessage() {
        return customNewUsernameMessage;
    }

    public boolean isCustomNewUsernameMessage() {
        return isCustomNewUsernameMessage;
    }

    public boolean isCustomServerFullMessage() {
        return config.getBoolean("use-custom-server-full-message", true);
    }

    public boolean isCustomWhitelistMessage() {
        return config.getBoolean("use-custom-whitelist-message", true);
    }

    public int getJoinQuitMessagePlayerCount() {
        return config.getInt("hide-join-quit-messages-above", -1);
    }

    public boolean hasJoinQuitMessagePlayerCount() {
        return getJoinQuitMessagePlayerCount() >= 0;
    }

    public boolean isNotifyNoNewMail() {
        return config.getBoolean("notify-no-new-mail", true);
    }

    public int getNotifyPlayerOfMailCooldown() {
        return config.getInt("notify-player-of-mail-cooldown", 0);
    }

    public int getMailsPerMinute() {
        return mailsPerMinute;
    }

    public int getMotdDelay() {
        return config.getInt("delay-motd", 0);
    }

    public boolean isSendFlyEnableOnJoin() {
        return config.getBoolean("send-fly-enable-on-join", true);
    }

    public boolean isWorldChangeFlyResetEnabled() {
        return config.getBoolean("world-change-fly-reset", true);
    }

    public boolean isWorldChangePreserveFlying() {
        return config.getBoolean("world-change-preserve-flying", true);
    }

    public boolean isGamemodeChangePreserveFlying() {
        return config.getBoolean("gamemode-change-preserve-flying", false);
    }

    public boolean isWorldChangeSpeedResetEnabled() {
        return config.getBoolean("world-change-speed-reset", true);
    }

    public double getMaxFlySpeed() {
        final double maxSpeed = config.getDouble("max-fly-speed", 0.8);
        return maxSpeed > 1.0 ? 1.0 : Math.abs(maxSpeed);
    }

    public double getMaxWalkSpeed() {
        final double maxSpeed = config.getDouble("max-walk-speed", 0.8);
        return maxSpeed > 1.0 ? 1.0 : Math.abs(maxSpeed);
    }

    // ------------------------------------------------------------ messaging

    public boolean isLastMessageReplyRecipient() {
        return config.getBoolean("last-message-reply-recipient", false);
    }

    public boolean isReplyToVanished() {
        return config.getBoolean("last-message-reply-vanished", true);
    }

    public long getLastMessageReplyRecipientTimeout() {
        return config.getLong("last-message-reply-recipient-timeout", 180);
    }

    // ------------------------------------------------------------ moderation

    public long getMaxMute() {
        return config.getLong("max-mute-time", -1);
    }

    public long getMaxTempban() {
        return config.getLong("max-tempban-time", -1);
    }

    public boolean isWorldTimePermissions() {
        return config.getBoolean("world-time-permissions", false);
    }

    public boolean getPerWarpPermission() {
        return config.getBoolean("per-warp-permission", false);
    }

    private TeleportWhenFreePolicy _getTeleportWhenFreePolicy() {
        if (config.hasProperty("teleport-back-when-freed-from-jail")) {
            return config.getBoolean("teleport-back-when-freed-from-jail", true) ? TeleportWhenFreePolicy.BACK : TeleportWhenFreePolicy.OFF;
        }
        if (config.hasProperty("teleport-when-freed")) {
            final String value = config.getString("teleport-when-freed", "back").replace("false", "off");
            try {
                return TeleportWhenFreePolicy.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                ess.getLogger().error("Invalid value \"" + value + "\" for config option \"teleport-when-freed\"!");
                return TeleportWhenFreePolicy.BACK;
            }
        }
        return TeleportWhenFreePolicy.BACK;
    }

    public TeleportWhenFreePolicy getTeleportWhenFreePolicy() {
        return teleportWhenFreePolicy;
    }

    public boolean isJailOnlineTime() {
        return config.getBoolean("jail-online-time", false);
    }

    public boolean isCompassTowardsHomePerm() {
        return isCompassTowardsHomePerm;
    }

    public boolean isAllowWorldInBroadcastworld() {
        return isAllowWorldInBroadcastworld;
    }

    // ------------------------------------------------------------ colors

    public String getPrimaryColor() {
        return primaryColor;
    }

    private String _getPrimaryColor() {
        final String color = config.getString("message-colors.primary", "#ffaa00");
        final String textColor = _getTagColor(color);
        return textColor != null ? textColor : DEFAULT_PRIMARY_COLOR;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    private String _getSecondaryColor() {
        final String color = config.getString("message-colors.secondary", "#ff5555");
        final String textColor = _getTagColor(color);
        return textColor != null ? textColor : DEFAULT_SECONDARY_COLOR;
    }

    private String _getTagColor(final String color) {
        try {
            if (color.startsWith("#") && color.length() == 7 && NumberUtil.isHexadecimal(color.substring(1))) {
                Integer.decode(color);
                return color;
            }
            if (color.length() == 1) {
                return Objects.requireNonNull(Text.fromChar(color.charAt(0))).toString();
            }
            return Objects.requireNonNull(NamedTextColor.NAMES.value(color.toLowerCase(Locale.ENGLISH))).toString();
        } catch (final NullPointerException | IllegalArgumentException ignored) {
        }
        return null;
    }

    /** Generic accessor used by the spawn module and diagnostics. */
    public String getString(final String path, final String def) {
        return config.getString(path, def);
    }

    public boolean getBoolean(final String path, final boolean def) {
        return config.getBoolean(path, def);
    }
}
