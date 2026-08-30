package net.essentialsx.fabric.spawn;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.kit.Kit;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.SimpleTextInput;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * EssentialsSpawn parity (Section 15): {@code /spawn}, {@code /setspawn}, first-join and
 * join-spawn routing, and the respawn destination resolver.
 */
public final class EssentialsSpawnFabric implements DedicatedServerModInitializer {
    public static final String MOD_ID = "essentials_fabric_spawn";
    public static final Logger LOGGER = LoggerFactory.getLogger("EssentialsSpawn");
    private Essentials ess;
    private SpawnStorage spawns;

    @Override
    public void onInitializeServer() {
        final String coreVersion = FabricLoader.getInstance().getModContainer(EssentialsFabric.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse(null);
        final String spawnVersion = FabricLoader.getInstance().getModContainer(MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse(null);
        if (coreVersion == null) {
            throw new IllegalStateException("Essentials Fabric Spawn requires the Essentials Fabric core mod");
        }
        if (!coreVersion.equals(spawnVersion)) {
            LOGGER.warn(Text.get().miniToLegacy(tlLiteral("versionMismatchAll")));
        }
        EssentialsFabric.onEssentialsReady(this::setup);
    }

    private void setup(final Essentials ess) {
        this.ess = ess;
        spawns = new SpawnStorage(ess);
        ess.addReloadable(spawns::reloadConfig);
        ess.getCommandRegistry().register(new Commandspawn(spawns), List.of("espawn"), true, "Teleport to the spawnpoint.", "/<command> [player] [world]");
        ess.getCommandRegistry().register(new Commandsetspawn(spawns), List.of("esetspawn"), true, "Sets the spawn point to your current position.", "/<command> <group>");
        ess.setRespawnResolver(this::resolveRespawn);
        ess.addJoinListener(this::onUserDataLoad);
        LOGGER.info("Essentials Spawn enabled");
    }

    public SpawnStorage getSpawns() {
        return spawns;
    }

    // ------------------------------------------------------------ respawn (Section 15.4)

    private LazyLocation resolveRespawn(final ServerPlayer player, final boolean death) {
        final User user = ess.getUser(player);
        if (user.isJailed() && user.getJail() != null && !user.getJail().isEmpty()) {
            return null;
        }
        if (ess.getSettings().getRespawnAtHome()) {
            LazyLocation home = null;
            if (ess.getSettings().isRespawnAtBed()) {
                home = vanillaRespawn(player);
            }
            if (home == null) {
                home = user.getHome(user.getLocation());
            }
            if (home != null) {
                return home;
            }
        }
        if (death && ess.getRandomTeleport().hasLocation(ess.getSettings().getRandomRespawnLocation())) {
            // Random respawn: resolved asynchronously after vanilla placement.
            final User target = user;
            ess.getRandomTeleport().getRandomLocation(ess.getSettings().getRandomRespawnLocation()).thenAccept(location -> {
                final CompletableFuture<Boolean> future = new CompletableFuture<>();
                target.getAsyncTeleport().now(location, false, TeleportCause.PLUGIN, future);
            });
            return null;
        }
        return spawns.getSpawn(user.getGroup());
    }

    /**
     * Vanilla bed/anchor respawn position honouring anchor charge and dimension rules.
     */
    private LazyLocation vanillaRespawn(final ServerPlayer player) {
        final BlockPos pos = player.getRespawnPosition();
        if (pos == null) {
            return null;
        }
        final ServerLevel level = ess.getServer().getLevel(player.getRespawnDimension());
        if (level == null) {
            return null;
        }
        final boolean isAnchor = level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.RespawnAnchorBlock;
        if (isAnchor && !ess.getSettings().isRespawnAtAnchor()) {
            return null;
        }
        final net.minecraft.world.level.portal.DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
        if (transition.missingRespawnBlock()) {
            return null;
        }
        return LazyLocation.of(transition.newLevel(), transition.pos().x, transition.pos().y, transition.pos().z, transition.yRot(), transition.xRot());
    }

    // ------------------------------------------------------------ join (Section 15.3)

    private void onUserDataLoad(final User user, final boolean firstJoin) {
        final ServerPlayer player = user.getBase();
        if (player == null) {
            return;
        }
        if (!firstJoin) {
            final List<String> spawnOnJoinGroups = ess.getSettings().getSpawnOnJoinGroups();
            if (!spawnOnJoinGroups.isEmpty()) {
                if (ess.getSettings().isUserInSpawnOnJoinGroup(user) && !user.isAuthorized("essentials.spawn-on-join.exempt")) {
                    ess.scheduleSyncDelayedTask(() -> {
                        final LazyLocation spawn = spawns.getSpawn(user.getGroup());
                        if (spawn == null) {
                            return;
                        }
                        final CompletableFuture<Boolean> future = new CompletableFuture<>();
                        future.exceptionally(e -> {
                            ess.showError(user.getSource(), e, "spawn-on-join");
                            return false;
                        });
                        user.getAsyncTeleport().nowUnsafe(spawn, TeleportCause.PLUGIN, future);
                    });
                }
            }
            return;
        }
        final boolean spawnRandomly = tryRandomTeleport(user, ess.getSettings().getRandomSpawnLocation());
        if (!spawnRandomly && !"none".equalsIgnoreCase(ess.getSettings().getNewbieSpawn())) {
            ess.scheduleSyncDelayedTask(() -> {
                if (!user.isOnline()) {
                    return;
                }
                final LazyLocation spawn = spawns.getSpawn(ess.getSettings().getNewbieSpawn());
                if (spawn != null) {
                    final CompletableFuture<Boolean> future = new CompletableFuture<>();
                    future.exceptionally(e -> {
                        LOGGER.warn(Text.get().miniToLegacy(tlLiteral("teleportNewPlayerError")), e);
                        return false;
                    });
                    user.getAsyncTeleport().now(spawn, false, TeleportCause.PLUGIN, future);
                }
            }, 1);
        }
        ess.scheduleSyncDelayedTask(() -> {
            if (!user.isOnline()) {
                return;
            }
            if (ess.getSettings().getAnnounceNewPlayers()) {
                final IText output = new KeywordReplacer(new SimpleTextInput(ess.getSettings().getAnnounceNewPlayerFormat()), user.getSource(), ess);
                for (final String line : output.getLines()) {
                    ess.broadcastMessage(line);
                }
            }
            final String kitName = ess.getSettings().getNewPlayerKit();
            if (!kitName.isEmpty() && !Boolean.TRUE.equals(user.getConfigMap().get("newbie-kit-granted"))) {
                try {
                    final Kit kit = new Kit(kitName.toLowerCase(Locale.ENGLISH), ess);
                    kit.expandItems(user);
                    // Idempotent grant marker (Section 15.3 step 4)
                    user.setConfigProperty("newbie-kit-granted", true);
                } catch (final Exception ex) {
                    LOGGER.warn(ex.getMessage());
                }
            }
        }, 2);
    }

    private boolean tryRandomTeleport(final User user, final String name) {
        if (!ess.getRandomTeleport().hasLocation(name)) {
            return false;
        }
        ess.getRandomTeleport().getRandomLocation(name).thenAccept(location -> {
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            user.getAsyncTeleport().now(location, false, TeleportCause.PLUGIN, future);
        });
        return true;
    }
}
