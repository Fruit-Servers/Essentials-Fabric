package net.essentialsx.fabric.backup;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Opt-in external backup process (Section 21.1): executable + argument array, no shell
 * interpolation, server-save coordination, bounded runtime and captured output.
 */
public class Backup implements Runnable {
    private static final long TIMEOUT_MINUTES = 60;
    private final transient Essentials ess;
    private final AtomicBoolean pendingShutdown = new AtomicBoolean(false);
    private transient boolean running = false;
    private transient boolean active = false;
    private transient long nextRunTick = -1;
    private transient long intervalTicks = -1;
    private transient CompletableFuture<Object> taskLock = null;

    public Backup(final Essentials ess) {
        this.ess = ess;
    }

    public void onServerStarted() {
        if (!ess.getOnlinePlayers().isEmpty() || ess.getSettings().isAlwaysRunBackup()) {
            startTask();
        }
    }

    public void onPlayerJoin() {
        startTask();
    }

    public synchronized void stopTask() {
        running = false;
        nextRunTick = -1;
    }

    private synchronized void startTask() {
        if (!running) {
            final long interval = ess.getSettings().getBackupInterval() * 1200;
            if (interval < 1200) {
                return;
            }
            intervalTicks = interval;
            nextRunTick = ess.getServer().getTickCount() + interval;
            running = true;
        }
    }

    /** Called every tick. */
    public void tick(final long serverTick) {
        if (running && nextRunTick > 0 && serverTick >= nextRunTick) {
            nextRunTick = serverTick + intervalTicks;
            run();
        }
    }

    public CompletableFuture<Object> getTaskLock() {
        return taskLock;
    }

    public void setPendingShutdown(final boolean shutdown) {
        pendingShutdown.set(shutdown);
    }

    @Override
    public void run() {
        if (active) {
            return;
        }
        final String command = ess.getSettings().getBackupCommand();
        if (command == null || command.isEmpty()) {
            return;
        }
        active = true;
        taskLock = new CompletableFuture<>();
        if ("save-all".equalsIgnoreCase(command)) {
            ess.getServer().saveEverything(true, false, false);
            active = false;
            taskLock.complete(new Object());
            return;
        }
        final List<String> argv = new ArrayList<>(List.of(command.trim().split("\\s+")));
        final Path executable = Path.of(argv.get(0));
        if (!executable.isAbsolute() || !Files.isExecutable(executable)) {
            ess.getLogger().error("backup.command must be an absolute path to an executable (got '{}'); shell interpolation is not supported.", argv.get(0));
            active = false;
            taskLock.complete(new Object());
            return;
        }
        ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("backupStarted")));
        ess.getServer().saveEverything(true, false, false);
        for (final net.minecraft.server.level.ServerLevel level : ess.getServer().getAllLevels()) {
            level.noSave = true;
        }
        ess.runTaskAsynchronously(() -> {
            try {
                final ProcessBuilder childBuilder = new ProcessBuilder(argv);
                childBuilder.redirectErrorStream(true);
                childBuilder.directory(ess.getServer().getServerDirectory().toFile());
                final Process child = childBuilder.start();
                ess.runTaskAsynchronously(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            ess.getLogger().info(line);
                        }
                    } catch (final IOException ex) {
                        ess.getLogger().error("An error occurred while reading backup child process", ex);
                    }
                });
                if (!child.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                    ess.getLogger().error("Backup process exceeded {} minutes and was terminated", TIMEOUT_MINUTES);
                    child.destroyForcibly();
                }
            } catch (final InterruptedException | IOException ex) {
                ess.getLogger().error("An error occurred while building the backup child process", ex);
            } finally {
                if (!pendingShutdown.get()) {
                    ess.scheduleSyncDelayedTask(() -> {
                        for (final net.minecraft.server.level.ServerLevel level : ess.getServer().getAllLevels()) {
                            level.noSave = false;
                        }
                        if (!ess.getSettings().isAlwaysRunBackup() && ess.getOnlinePlayers().isEmpty()) {
                            stopTask();
                        }
                        active = false;
                        taskLock.complete(new Object());
                        ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("backupFinished")));
                    });
                } else {
                    active = false;
                    taskLock.complete(new Object());
                }
            }
        });
    }
}
