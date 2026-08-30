package net.essentialsx.fabric;

import net.essentialsx.fabric.backup.Backup;
import net.essentialsx.fabric.command.CommandRegistry;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.config.AsyncWriter;
import net.essentialsx.fabric.config.Settings;
import net.essentialsx.fabric.economy.BalanceTop;
import net.essentialsx.fabric.economy.ImpactorEconomy;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Enchantments;
import net.essentialsx.fabric.items.ItemDb;
import net.essentialsx.fabric.items.ItemSerializer;
import net.essentialsx.fabric.jail.Jails;
import net.essentialsx.fabric.kit.Kits;
import net.essentialsx.fabric.listener.DisplayNames;
import net.essentialsx.fabric.listener.PlayerTimeWeather;
import net.essentialsx.fabric.listener.SleepManager;
import net.essentialsx.fabric.listener.TickTimer;
import net.essentialsx.fabric.listener.Visibility;
import net.essentialsx.fabric.mail.MailService;
import net.essentialsx.fabric.perm.PermissionsHandler;
import net.essentialsx.fabric.rtp.RandomTeleport;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserMap;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.warp.Warps;
import net.essentialsx.fabric.worth.Worth;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Service container for the core mod (Section 4.3). One instance per server lifetime.
 */
public final class Essentials {
    public static final String VERSION = FabricLoader.getInstance().getModContainer(EssentialsFabric.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");

    private final Logger logger;
    private final Path dataFolder;
    private MinecraftServer server;
    private Settings settings;
    private I18n i18n;
    private UserMap userMap;
    private PermissionsHandler permissionsHandler;
    private ImpactorEconomy economy;
    private BalanceTop balanceTop;
    private ItemDb itemDb;
    private ItemSerializer itemSerializer;
    private Kits kits;
    private Warps warps;
    private Worth worth;
    private Jails jails;
    private RandomTeleport randomTeleport;
    private MailService mail;
    private Backup backup;
    private TickTimer timer;
    private CommandRegistry commandRegistry;
    private DisplayNames displayNames;
    private SleepManager sleepManager;
    private PlayerTimeWeather playerTimeWeather;
    private Visibility visibility;
    private final Set<String> vanishedPlayers = new LinkedHashSet<>();
    private final ConcurrentLinkedQueue<Runnable> mainThreadQueue = new ConcurrentLinkedQueue<>();
    private final List<DelayedTask> delayedTasks = new ArrayList<>();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2, r -> {
        final Thread t = new Thread(r, "Essentials-Async");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean stopping = false;
    private RespawnResolver respawnResolver;
    private final List<Reloadable> reloadables = new ArrayList<>();

    public Essentials(final Logger logger, final Path dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    // ------------------------------------------------------------ lifecycle

    /**
     * Initialise everything that does not need a running server (config, locale, item db).
     */
    public void init() {
        try {
            Files.createDirectories(dataFolder);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to create Essentials data folder", e);
        }
        i18n = new I18n(this);
        i18n.onEnable();
        Console.setInstance(this);
        settings = new Settings(this);
        reloadables.add(settings::reloadConfig);
        Text.install(new Text(settings.getPrimaryColor(), settings.getSecondaryColor()));
        i18n.updateLocale(settings.getLocale());
        userMap = new UserMap(this);
        permissionsHandler = new PermissionsHandler(this);
        itemDb = new ItemDb(this);
        itemDb.reloadConfig();
        reloadables.add(itemDb::reloadConfig);
        settings._lateLoadItemSpawnBlacklist();
        itemSerializer = new ItemSerializer(this);
        kits = new Kits(this);
        reloadables.add(kits::reloadConfig);
        warps = new Warps(this, dataFolder);
        reloadables.add(warps::reloadConfig);
        worth = new Worth(dataFolder);
        reloadables.add(worth::reloadConfig);
        jails = new Jails(this);
        reloadables.add(jails::reloadConfig);
        randomTeleport = new RandomTeleport(this);
        reloadables.add(randomTeleport::reloadConfig);
        mail = new MailService(this);
        displayNames = new DisplayNames(this);
        sleepManager = new SleepManager(this);
        playerTimeWeather = new PlayerTimeWeather(this);
        visibility = new Visibility(this);
        commandRegistry = new CommandRegistry(this);
        timer = new TickTimer(this);
        backup = new Backup(this);
        logger.info("Essentials Fabric {} loaded configuration from {}", VERSION, dataFolder);
    }

    /**
     * Bind server-dependent services (Impactor economy, registries).
     */
    public void onServerStarting(final MinecraftServer server) {
        this.server = server;
        this.stopping = false;
        Text.get().setRegistries(server.registryAccess());
        Enchantments.setRegistries(server.registryAccess());
        try {
            economy = ImpactorEconomy.bind(this);
            logger.info("Bound Impactor economy currency '{}'", economy.currencyName());
        } catch (final IllegalStateException ex) {
            economy = null;
            logger.error("==============================================================");
            logger.error("Essentials Fabric requires Impactor 5.3.5+ with a configured currency.");
            logger.error("{}", ex.getMessage());
            logger.error("The server will now stop (Section 5.1 of the technical design).");
            logger.error("==============================================================");
            throw ex;
        }
        balanceTop = new BalanceTop(this);
    }

    public void onServerStarted(final MinecraftServer server) {
        backup.onServerStarted();
    }

    public void onServerStopping(final MinecraftServer server) {
        stopping = true;
        for (final User user : getOnlineUsers()) {
            if (user.isVanished()) {
                user.setVanished(false);
                user.sendTl("unvanishedReload");
            }
            user.setLogoutLocation();
            if (!user.isHidden()) {
                user.setLastLogout(System.currentTimeMillis());
            }
            user.onSessionEnd(server.getTickCount());
        }
        backup.stopTask();
        if (backup.getTaskLock() != null && !backup.getTaskLock().isDone()) {
            logger.error(Text.get().miniToLegacy(tlLiteral("backupInProgress")));
            backup.getTaskLock().join();
        }
        Trade.closeLog();
        userMap.shutdown();
        asyncExecutor.shutdown();
        if (!AsyncWriter.shutdown(30, TimeUnit.SECONDS)) {
            logger.error("Essentials could not confirm durability of all user data before shutdown!");
        }
        permissionsHandler.unregisterContexts();
        i18n.onDisable();
    }

    public boolean isStopping() {
        return stopping;
    }

    /**
     * Two-phase reload of reload-safe configuration (Section 6.4).
     */
    public void reload() {
        Trade.closeLog();
        for (final Reloadable reloadable : reloadables) {
            try {
                reloadable.reload();
            } catch (final Exception ex) {
                logger.error("Reload failed for {}: {}", reloadable.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
        Text.install(new Text(settings.getPrimaryColor(), settings.getSecondaryColor()));
        if (server != null) {
            Text.get().setRegistries(server.registryAccess());
        }
        i18n.updateLocale(settings.getLocale());
        settings._lateLoadItemSpawnBlacklist();
        if (server != null) {
            for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
            }
        }
    }

    // ------------------------------------------------------------ accessors

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Settings getSettings() {
        return settings;
    }

    public I18n getI18n() {
        return i18n;
    }

    public UserMap getUsers() {
        return userMap;
    }

    public PermissionsHandler getPermissionsHandler() {
        return permissionsHandler;
    }

    public ImpactorEconomy getEconomy() {
        return economy;
    }

    public BalanceTop getBalanceTop() {
        return balanceTop;
    }

    public ItemDb getItemDb() {
        return itemDb;
    }

    public ItemSerializer getItemSerializer() {
        return itemSerializer;
    }

    public Kits getKits() {
        return kits;
    }

    public Warps getWarps() {
        return warps;
    }

    public Worth getWorth() {
        return worth;
    }

    public Jails getJails() {
        return jails;
    }

    public RandomTeleport getRandomTeleport() {
        return randomTeleport;
    }

    public MailService getMail() {
        return mail;
    }

    public Backup getBackup() {
        return backup;
    }

    public TickTimer getTimer() {
        return timer;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public DisplayNames getDisplayNames() {
        return displayNames;
    }

    public SleepManager getSleepManager() {
        return sleepManager;
    }

    public PlayerTimeWeather getPlayerTimeWeather() {
        return playerTimeWeather;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Set<String> getVanishedPlayersNew() {
        return vanishedPlayers;
    }

    public void addReloadable(final Reloadable reloadable) {
        reloadables.add(reloadable);
    }

    public void setRespawnResolver(final RespawnResolver resolver) {
        this.respawnResolver = resolver;
    }

    public RespawnResolver getRespawnResolver() {
        return respawnResolver;
    }

    private final List<JoinListener> joinListeners = new ArrayList<>();

    /** Register a listener invoked once user data is loaded after a player joins (Section 15.3). */
    public void addJoinListener(final JoinListener listener) {
        joinListeners.add(listener);
    }

    public void fireJoin(final User user, final boolean firstJoin) {
        for (final JoinListener listener : joinListeners) {
            try {
                listener.onJoin(user, firstJoin);
            } catch (final Throwable t) {
                logger.error("Join listener failed", t);
            }
        }
    }

    @FunctionalInterface
    public interface JoinListener {
        void onJoin(User user, boolean firstJoin);
    }

    // ------------------------------------------------------------ users

    public User getUser(final ServerPlayer player) {
        return userMap.getUser(player);
    }

    public User getUser(final UUID uuid) {
        return userMap.getUser(uuid);
    }

    public User getUser(final String name) {
        return userMap.getUser(name);
    }

    public User getOfflineUser(final String name) {
        return userMap.getUser(name);
    }

    public Collection<User> getOnlineUsers() {
        return userMap.getOnlineUsers();
    }

    public List<ServerPlayer> getOnlinePlayers() {
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    public List<ServerPlayer> getJailedPlayers() {
        final List<ServerPlayer> list = new ArrayList<>();
        for (final User user : getOnlineUsers()) {
            if (user.isJailed() && user.getBase() != null) {
                list.add(user.getBase());
            }
        }
        return list;
    }

    /**
     * Upstream {@code matchUser}: exact online, then partial online, then offline names.
     */
    public User matchUser(final User sourceUser, final String searchTerm, final Boolean getHidden, final boolean getOffline) throws net.essentialsx.fabric.command.PlayerNotFoundException {
        if (sourceUser != null && (searchTerm.equals("@p") || searchTerm.equals("@s"))) {
            return sourceUser;
        }
        ServerPlayer exPlayer = null;
        try {
            exPlayer = server.getPlayerList().getPlayer(UUID.fromString(searchTerm));
        } catch (final IllegalArgumentException ex) {
            exPlayer = server.getPlayerList().getPlayerByName(searchTerm);
            if (exPlayer == null && !getOffline) {
                exPlayer = matchPlayerPrefix(searchTerm);
            }
        }
        final User user;
        if (exPlayer != null) {
            user = getUser(exPlayer);
        } else {
            user = getUser(searchTerm);
        }
        if (user != null) {
            if (!getOffline && !user.isOnline()) {
                throw new net.essentialsx.fabric.command.PlayerNotFoundException();
            }
            if (getHidden || canInteractWith(sourceUser, user)) {
                return user;
            } else {
                if (getOffline && user.getName().equalsIgnoreCase(searchTerm)) {
                    return user;
                }
            }
            throw new net.essentialsx.fabric.command.PlayerNotFoundException();
        }
        final List<ServerPlayer> matches = matchPlayers(searchTerm);
        if (matches.isEmpty()) {
            final String matchText = searchTerm.toLowerCase(Locale.ENGLISH);
            for (final User userMatch : getOnlineUsers()) {
                if (getHidden || canInteractWith(sourceUser, userMatch)) {
                    final String displayName = FormatUtil.stripFormat(userMatch.getDisplayName()).toLowerCase(Locale.ENGLISH);
                    if (displayName.contains(matchText)) {
                        return userMatch;
                    }
                }
            }
        } else {
            for (final ServerPlayer player : matches) {
                if (player.getGameProfile().getName().equalsIgnoreCase(searchTerm)) {
                    final User userMatch = getUser(player);
                    if (getHidden || canInteractWith(sourceUser, userMatch)) {
                        return userMatch;
                    }
                }
            }
            for (final ServerPlayer player : matches) {
                final User userMatch = getUser(player);
                if (userMatch.getDisplayName().startsWith(searchTerm) && (getHidden || canInteractWith(sourceUser, userMatch))) {
                    return userMatch;
                }
            }
            final User userMatch = getUser(matches.get(0));
            if (getHidden || canInteractWith(sourceUser, userMatch)) {
                return userMatch;
            }
        }
        throw new net.essentialsx.fabric.command.PlayerNotFoundException();
    }

    private ServerPlayer matchPlayerPrefix(final String term) {
        final List<ServerPlayer> matches = matchPlayers(term);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Bukkit {@code Server#matchPlayer}: exact match first, otherwise every player whose name
     * starts with the term.
     */
    public List<ServerPlayer> matchPlayers(final String term) {
        final List<ServerPlayer> matched = new ArrayList<>();
        final String lower = term.toLowerCase(Locale.ENGLISH);
        for (final ServerPlayer player : getOnlinePlayers()) {
            final String name = player.getGameProfile().getName();
            if (name.equalsIgnoreCase(term)) {
                matched.clear();
                matched.add(player);
                break;
            }
            if (name.toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                matched.add(player);
            }
        }
        return matched;
    }

    public boolean canInteractWith(final CommandSource interactor, final User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }
        if (interactor.isPlayer()) {
            return canInteractWith(getUser(interactor.getPlayer()), interactee);
        }
        return true;
    }

    public boolean canInteractWith(final User interactor, final User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }
        if (interactor.equals(interactee)) {
            return true;
        }
        return !interactee.isHiddenFrom(interactor.getBase());
    }

    // ------------------------------------------------------------ scheduling (Section 4.4)

    public void scheduleSyncDelayedTask(final Runnable runnable) {
        if (server != null && server.isSameThread()) {
            mainThreadQueue.add(runnable);
        } else {
            mainThreadQueue.add(runnable);
        }
    }

    public void scheduleSyncDelayedTask(final Runnable runnable, final int delayTicks) {
        if (delayTicks <= 0) {
            scheduleSyncDelayedTask(runnable);
            return;
        }
        synchronized (delayedTasks) {
            delayedTasks.add(new DelayedTask(runnable, (server == null ? 0 : server.getTickCount()) + delayTicks));
        }
    }

    public void runTaskAsynchronously(final Runnable runnable) {
        if (asyncExecutor.isShutdown()) {
            runnable.run();
            return;
        }
        asyncExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (final Throwable t) {
                logger.error("Async task failed", t);
            }
        });
    }

    public <T> CompletableFuture<T> supplyAsync(final java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    /** Called every server tick from the tick event. */
    public void drainMainThreadQueue() {
        Runnable task;
        int budget = 1000;
        while ((task = mainThreadQueue.poll()) != null && budget-- > 0) {
            try {
                task.run();
            } catch (final Throwable t) {
                logger.error("Scheduled task failed", t);
            }
        }
        if (!delayedTasks.isEmpty()) {
            final long tick = server.getTickCount();
            final List<DelayedTask> due = new ArrayList<>();
            synchronized (delayedTasks) {
                delayedTasks.removeIf(dt -> {
                    if (dt.tick <= tick) {
                        due.add(dt);
                        return true;
                    }
                    return false;
                });
            }
            for (final DelayedTask dt : due) {
                try {
                    dt.runnable.run();
                } catch (final Throwable t) {
                    logger.error("Delayed task failed", t);
                }
            }
        }
    }

    // ------------------------------------------------------------ broadcasting / messaging

    public void broadcastTl(final String tlKey, final Object... args) {
        broadcastTl(null, null, tlKey, args);
    }

    public void broadcastTl(final User sender, final Predicate<User> shouldExclude, final String tlKey, final Object... args) {
        for (final User user : getOnlineUsers()) {
            if (shouldExclude != null && shouldExclude.test(user)) {
                continue;
            }
            if (sender != null && user.isIgnoredPlayer(sender)) {
                continue;
            }
            user.sendTl(tlKey, args);
        }
        if (server != null) {
            final String translation = tlLiteral(tlKey, args);
            if (!translation.isEmpty()) {
                logger.info(Text.get().miniToPlain(translation));
            }
        }
    }

    public void broadcastMessage(final String legacyMessage) {
        if (server == null || legacyMessage == null || legacyMessage.isEmpty()) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(Text.get().legacy(legacyMessage), false);
    }

    public void broadcastComponent(final net.minecraft.network.chat.Component component) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(component, false);
        }
    }

    public void showError(final CommandSource sender, final Throwable exception, final String commandLabel) {
        if (exception instanceof TranslatableException) {
            final String tlMessage = sender.tl(((TranslatableException) exception).getTlKey(), ((TranslatableException) exception).getArgs());
            sender.sendTl("errorWithMessage", Text.parsed(tlMessage));
        } else {
            sender.sendTl("errorWithMessage", exception.getMessage());
        }
        if (getSettings().isDebug()) {
            logger.info(Text.get().miniToLegacy(tlLiteral("errorCallingCommand", commandLabel)), exception);
        }
    }

    public void dispatchConsoleCommand(final String command) {
        if (server == null) {
            return;
        }
        final String cmd = command.startsWith("/") ? command.substring(1) : command;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
    }

    public void dispatchCommand(final CommandSourceStack source, final String command) {
        if (server == null) {
            return;
        }
        final String cmd = command.startsWith("/") ? command.substring(1) : command;
        server.getCommands().performPrefixedCommand(source, cmd);
    }

    public void kickPlayer(final User user, final String legacyReason) {
        if (user.getBase() != null) {
            user.getBase().connection.disconnect(Text.get().legacy(legacyReason));
        }
    }

    /**
     * Respawn location for {@code /home}-style respawn teleports: the player's bed/anchor or
     * the world spawn (spawn module may override through {@link RespawnResolver}).
     */
    public LazyLocation getRespawnLocation(final ServerPlayer player) {
        if (respawnResolver != null) {
            final LazyLocation resolved = respawnResolver.resolve(player, false);
            if (resolved != null) {
                return resolved;
            }
        }
        if (player.getRespawnPosition() != null && server.getLevel(player.getRespawnDimension()) != null) {
            // "alive" transition: peeks the bed/anchor without consuming anchor charge.
            final net.minecraft.world.level.portal.DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
            if (!transition.missingRespawnBlock()) {
                return LazyLocation.of(transition.newLevel(), transition.pos().x, transition.pos().y, transition.pos().z, transition.yRot(), transition.xRot());
            }
        }
        return getWorldSpawn(server.overworld());
    }

    public LazyLocation getWorldSpawn(final ServerLevel level) {
        final BlockPos pos = level.getSharedSpawnPos();
        return LazyLocation.of(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getSharedSpawnAngle(), 0f);
    }

    public ServerLevel getOverworld() {
        return server.getLevel(Level.OVERWORLD);
    }

    @FunctionalInterface
    public interface Reloadable {
        void reload();
    }

    /**
     * Hook for the spawn module to contribute respawn destinations (Section 15.4).
     */
    @FunctionalInterface
    public interface RespawnResolver {
        LazyLocation resolve(ServerPlayer player, boolean death);
    }

    private static final class DelayedTask {
        final Runnable runnable;
        final long tick;

        DelayedTask(final Runnable runnable, final long tick) {
            this.runnable = runnable;
            this.tick = tick;
        }
    }
}
