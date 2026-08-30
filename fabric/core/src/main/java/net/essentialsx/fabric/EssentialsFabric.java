package net.essentialsx.fabric;

import net.essentialsx.fabric.listener.BlockListener;
import net.essentialsx.fabric.listener.EntityListener;
import net.essentialsx.fabric.listener.PlayerListener;
import net.essentialsx.fabric.signs.SignListener;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fabric entrypoint for the core mod. Wires Fabric lifecycle/tick/command events into the
 * {@link Essentials} service container and the listener classes.
 */
public final class EssentialsFabric implements DedicatedServerModInitializer {
    public static final String MOD_ID = "essentials_fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger("Essentials");
    private static Essentials instance;
    private static final List<Consumer<Essentials>> readyCallbacks = new ArrayList<>();
    private static PlayerListener playerListener;
    private static EntityListener entityListener;
    private static BlockListener blockListener;
    private static SignListener signListener;

    /** The active Essentials instance (null before the server starts). */
    public static Essentials get() {
        return instance;
    }

    public static PlayerListener players() {
        return playerListener;
    }

    public static EntityListener entities() {
        return entityListener;
    }

    public static BlockListener blocks() {
        return blockListener;
    }

    public static SignListener signs() {
        return signListener;
    }

    /**
     * Run a callback once the core has initialised (immediately if it already has).
     */
    public static synchronized void onEssentialsReady(final Consumer<Essentials> callback) {
        if (instance != null) {
            callback.accept(instance);
        } else {
            readyCallbacks.add(callback);
        }
    }

    @Override
    public void onInitializeServer() {
        final Path dataFolder = FabricLoader.getInstance().getConfigDir().resolve("essentials-fabric");
        final Essentials ess = new Essentials(LOGGER, dataFolder);
        ess.init();
        CommandRegistrar.registerAll(ess);
        playerListener = new PlayerListener(ess);
        entityListener = new EntityListener(ess);
        blockListener = new BlockListener(ess);
        signListener = new SignListener(ess);
        synchronized (EssentialsFabric.class) {
            instance = ess;
            for (final Consumer<Essentials> callback : readyCallbacks) {
                callback.accept(ess);
            }
            readyCallbacks.clear();
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ess.getCommandRegistry().registerAll(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(ess::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(ess::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ess::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ess.drainMainThreadQueue();
            ess.getTimer().tick();
            ess.getBackup().tick(server.getTickCount());
            ess.getPlayerTimeWeather().tick(server.getTickCount());
        });

        playerListener.register();
        entityListener.register();
        blockListener.register();
        LOGGER.info("Essentials Fabric {} initialised", Essentials.VERSION);
    }
}
