package net.essentialsx.fabric.textreader;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.PlayerList;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.DescParseTickFormat;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum KeywordType {
    PLAYER(KeywordCachable.CACHEABLE),
    DISPLAYNAME(KeywordCachable.CACHEABLE),
    USERNAME(KeywordCachable.NOTCACHEABLE),
    NICKNAME(KeywordCachable.CACHEABLE),
    PREFIX(KeywordCachable.CACHEABLE),
    SUFFIX(KeywordCachable.CACHEABLE),
    GROUP(KeywordCachable.CACHEABLE),
    BALANCE(KeywordCachable.CACHEABLE),
    MAILS(KeywordCachable.CACHEABLE),
    PLAYTIME(KeywordCachable.CACHEABLE),
    WORLD(KeywordCachable.CACHEABLE),
    WORLDNAME(KeywordCachable.CACHEABLE),
    ONLINE(KeywordCachable.CACHEABLE),
    UNIQUE(KeywordCachable.CACHEABLE),
    WORLDS(KeywordCachable.CACHEABLE),
    PLAYERLIST(KeywordCachable.SUBVALUE, true),
    TIME(KeywordCachable.CACHEABLE),
    DATE(KeywordCachable.CACHEABLE),
    WORLDTIME12(KeywordCachable.CACHEABLE),
    WORLDTIME24(KeywordCachable.CACHEABLE),
    WORLDDATE(KeywordCachable.CACHEABLE),
    COORDS(KeywordCachable.CACHEABLE),
    TPS(KeywordCachable.CACHEABLE),
    UPTIME(KeywordCachable.CACHEABLE),
    IP(KeywordCachable.CACHEABLE, true),
    ADDRESS(KeywordCachable.CACHEABLE, true),
    PLUGINS(KeywordCachable.CACHEABLE, true),
    VERSION(KeywordCachable.CACHEABLE, true);

    private final KeywordCachable type;
    private final boolean isPrivate;

    KeywordType(final KeywordCachable type) {
        this.type = type;
        this.isPrivate = false;
    }

    KeywordType(final KeywordCachable type, final boolean isPrivate) {
        this.type = type;
        this.isPrivate = isPrivate;
    }

    public KeywordCachable getType() {
        return type;
    }

    public boolean isPrivate() {
        return isPrivate;
    }
}

enum KeywordCachable {
    CACHEABLE,
    SUBVALUE,
    NOTCACHEABLE
}

/**
 * Replaces {@code {KEYWORD}} placeholders in text pages, kits and MOTDs.
 */
public class KeywordReplacer implements IText {
    private static final Pattern KEYWORD = Pattern.compile("\\{([^\\{\\}]+)\\}");
    private static final Pattern KEYWORDSPLIT = Pattern.compile("\\:");
    private final transient IText input;
    private final transient List<String> replaced;
    private final transient Essentials ess;
    private final transient boolean includePrivate;
    private final transient boolean replaceSpacesWithUnderscores;
    private final EnumMap<KeywordType, Object> keywordCache = new EnumMap<>(KeywordType.class);

    public KeywordReplacer(final IText input, final CommandSource sender, final Essentials ess) {
        this(input, sender, ess, true, false);
    }

    public KeywordReplacer(final IText input, final CommandSource sender, final Essentials ess, final boolean showPrivate) {
        this(input, sender, ess, showPrivate, false);
    }

    public KeywordReplacer(final IText input, final CommandSource sender, final Essentials ess, final boolean showPrivate, final boolean replaceSpacesWithUnderscores) {
        this.input = input;
        this.replaced = new ArrayList<>(this.input.getLines().size());
        this.ess = ess;
        this.includePrivate = showPrivate;
        this.replaceSpacesWithUnderscores = replaceSpacesWithUnderscores;
        replaceKeywords(sender);
    }

    private void replaceKeywords(final CommandSource sender) {
        User user = null;
        if (sender != null && sender.isPlayer()) {
            user = ess.getUser(sender.getPlayer());
        }
        for (int i = 0; i < input.getLines().size(); i++) {
            String line = input.getLines().get(i);
            if (line.startsWith("@")) {
                replaced.add(line);
                continue;
            }
            final Matcher matcher = KEYWORD.matcher(line);
            while (matcher.find()) {
                final String fullMatch = matcher.group(0);
                final String keywordMatch = matcher.group(1);
                final String[] matchTokens = KEYWORDSPLIT.split(keywordMatch);
                line = replaceLine(line, fullMatch, matchTokens, user);
            }
            replaced.add(line);
        }
    }

    @SuppressWarnings("unchecked")
    private String replaceLine(String line, final String fullMatch, final String[] matchTokens, final User user) {
        final String keyword = matchTokens[0];
        try {
            String replacer = null;
            final KeywordType validKeyword = KeywordType.valueOf(keyword);
            if (validKeyword.getType().equals(KeywordCachable.CACHEABLE) && keywordCache.containsKey(validKeyword)) {
                replacer = keywordCache.get(validKeyword).toString();
            } else if (validKeyword.getType().equals(KeywordCachable.SUBVALUE)) {
                String subKeyword = "";
                if (matchTokens.length > 1) {
                    subKeyword = matchTokens[1].toLowerCase(Locale.ENGLISH);
                }
                if (keywordCache.containsKey(validKeyword)) {
                    final Map<String, String> values = (Map<String, String>) keywordCache.get(validKeyword);
                    if (values.containsKey(subKeyword)) {
                        replacer = values.get(subKeyword);
                    }
                }
            }
            if (validKeyword.isPrivate() && !includePrivate) {
                replacer = "";
            }
            if (replacer == null) {
                replacer = "";
                final ServerPlayer base = user == null ? null : user.getBase();
                switch (validKeyword) {
                    case PLAYER:
                    case DISPLAYNAME:
                        if (user != null) {
                            replacer = user.getDisplayName();
                        }
                        break;
                    case USERNAME:
                        if (user != null) {
                            replacer = user.getName();
                        }
                        break;
                    case NICKNAME:
                        if (user != null) {
                            final String nickname = user.getFormattedNickname();
                            replacer = nickname == null ? user.getName() : nickname;
                        }
                        break;
                    case PREFIX:
                        if (base != null) {
                            final String prefix = FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(base));
                            replacer = prefix == null ? "" : prefix;
                        }
                        break;
                    case SUFFIX:
                        if (base != null) {
                            final String suffix = FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(base));
                            replacer = suffix == null ? "" : suffix;
                        }
                        break;
                    case GROUP:
                        if (user != null) {
                            replacer = user.getGroup();
                        }
                        break;
                    case BALANCE:
                        if (user != null) {
                            replacer = Text.get().miniToLegacy(NumberUtil.displayCurrency(user.getMoney(), ess));
                        }
                        break;
                    case MAILS:
                        if (user != null) {
                            replacer = Integer.toString(user.getMailAmount());
                        }
                        break;
                    case PLAYTIME:
                        if (user != null) {
                            final long playtimeMs = System.currentTimeMillis() - (user.getPlaytimeTicksLive() * 50L);
                            replacer = DateUtil.formatDateDiff(playtimeMs);
                        }
                        break;
                    case WORLD:
                    case WORLDNAME:
                        if (user != null) {
                            final LazyLocation location = user.getLocation();
                            replacer = location == null ? "" : location.worldDisplayName(ess.getServer());
                        }
                        break;
                    case ONLINE:
                        int playerHidden = 0;
                        for (final User u : ess.getOnlineUsers()) {
                            if (u.isHidden()) {
                                playerHidden++;
                            }
                        }
                        replacer = Integer.toString(ess.getOnlinePlayers().size() - playerHidden);
                        break;
                    case UNIQUE:
                        replacer = NumberFormat.getInstance().format(ess.getUsers().getUserCount());
                        break;
                    case WORLDS:
                        final StringBuilder worldsBuilder = new StringBuilder();
                        for (final ServerLevel w : ess.getServer().getAllLevels()) {
                            if (worldsBuilder.length() > 0) {
                                worldsBuilder.append(", ");
                            }
                            worldsBuilder.append(Worlds.name(w));
                        }
                        replacer = worldsBuilder.toString();
                        break;
                    case PLAYERLIST:
                        final Map<String, String> outputList;
                        if (keywordCache.containsKey(validKeyword)) {
                            outputList = (Map<String, String>) keywordCache.get(validKeyword);
                        } else {
                            final boolean showHidden;
                            if (user == null) {
                                showHidden = true;
                            } else {
                                showHidden = user.isAuthorized("essentials.list.hidden") || user.canInteractVanished();
                            }
                            final Map<String, List<User>> playerList = PlayerList.getPlayerLists(ess, user, showHidden);
                            outputList = new HashMap<>();
                            for (final String groupName : playerList.keySet()) {
                                final List<User> groupUsers = playerList.get(groupName);
                                if (groupUsers != null && !groupUsers.isEmpty()) {
                                    outputList.put(groupName, Text.get().miniToLegacy(PlayerList.listUsers(ess, groupUsers, " ")));
                                }
                            }
                            final StringBuilder playerlistBuilder = new StringBuilder();
                            for (final User p : ess.getOnlineUsers()) {
                                if (p.isHidden()) {
                                    continue;
                                }
                                if (playerlistBuilder.length() > 0) {
                                    playerlistBuilder.append(", ");
                                }
                                playerlistBuilder.append(p.getDisplayName());
                            }
                            outputList.put("", playerlistBuilder.toString());
                            keywordCache.put(validKeyword, outputList);
                        }
                        if (matchTokens.length == 1) {
                            replacer = outputList.get("");
                        } else if (outputList.containsKey(matchTokens[1].toLowerCase(Locale.ENGLISH))) {
                            replacer = outputList.get(matchTokens[1].toLowerCase(Locale.ENGLISH));
                        } else if (matchTokens.length > 2) {
                            replacer = matchTokens[2];
                        }
                        keywordCache.put(validKeyword, outputList);
                        break;
                    case TIME:
                        replacer = DateFormat.getTimeInstance(DateFormat.MEDIUM, ess.getI18n().getCurrentLocale()).format(new Date());
                        break;
                    case DATE:
                        replacer = DateFormat.getDateInstance(DateFormat.MEDIUM, ess.getI18n().getCurrentLocale()).format(new Date());
                        break;
                    case WORLDTIME12:
                        if (base != null) {
                            replacer = DescParseTickFormat.format12(base.level().getDayTime());
                        }
                        break;
                    case WORLDTIME24:
                        if (base != null) {
                            replacer = DescParseTickFormat.format24(base.level().getDayTime());
                        }
                        break;
                    case WORLDDATE:
                        if (base != null) {
                            replacer = DateFormat.getDateInstance(DateFormat.MEDIUM, ess.getI18n().getCurrentLocale()).format(DescParseTickFormat.ticksToDate(base.level().getDayTime()));
                        }
                        break;
                    case COORDS:
                        if (user != null) {
                            final LazyLocation location = user.getLocation();
                            replacer = user.playerTl("coordsKeyword", location.blockX(), location.blockY(), location.blockZ());
                        }
                        break;
                    case TPS:
                        replacer = NumberUtil.formatDouble(ess.getTimer().getAverageTPS());
                        break;
                    case UPTIME:
                        replacer = DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime());
                        break;
                    case IP:
                        if (base != null) {
                            replacer = base.getIpAddress();
                        }
                        break;
                    case ADDRESS:
                        if (base != null) {
                            replacer = base.connection.getRemoteAddress() == null ? "" : base.connection.getRemoteAddress().toString();
                        }
                        break;
                    case PLUGINS:
                        final StringBuilder pluginlistBuilder = new StringBuilder();
                        for (final ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                            if (pluginlistBuilder.length() > 0) {
                                pluginlistBuilder.append(", ");
                            }
                            pluginlistBuilder.append(mod.getMetadata().getName());
                        }
                        replacer = pluginlistBuilder.toString();
                        break;
                    case VERSION:
                        replacer = "Fabric " + SharedConstants.getCurrentVersion().getName();
                        break;
                    default:
                        replacer = "N/A";
                        break;
                }
                if (this.replaceSpacesWithUnderscores) {
                    if (!line.startsWith("/") && validKeyword != KeywordType.USERNAME) {
                        replacer = replacer.replace("_", "\\_").replaceAll("\\s", "_");
                    }
                }
                if (validKeyword.getType().equals(KeywordCachable.CACHEABLE)) {
                    keywordCache.put(validKeyword, replacer);
                }
            }
            line = line.replace(fullMatch, replacer);
        } catch (final IllegalArgumentException ignored) {
        }
        return line;
    }

    @Override
    public List<String> getLines() {
        return replaced;
    }

    @Override
    public List<String> getChapters() {
        return input.getChapters();
    }

    @Override
    public Map<String, Integer> getBookmarks() {
        return input.getBookmarks();
    }
}
