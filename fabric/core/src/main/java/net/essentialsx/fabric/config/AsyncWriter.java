package net.essentialsx.fabric.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single ordered writer for administrator data files (Section 6.3): every write is a
 * snapshot encoded off-thread, written to a sibling temp file, flushed and atomically moved.
 * Repeated saves for the same path are coalesced so only the newest snapshot is written.
 */
public final class AsyncWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Essentials-IO");
    private static volatile ExecutorService EXECUTOR = newExecutor();
    private static final Map<Path, PendingWrite> PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger QUEUE_DEPTH = new AtomicInteger();

    private AsyncWriter() {
    }

    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "Essentials-Writer");
            t.setDaemon(true);
            return t;
        });
    }

    private static synchronized ExecutorService executor() {
        if (EXECUTOR == null || EXECUTOR.isShutdown()) {
            EXECUTOR = newExecutor();
        }
        return EXECUTOR;
    }

    public static int queueDepth() {
        return QUEUE_DEPTH.get();
    }

    /**
     * Queue an atomic write of {@code content} to {@code path}. If a write for the same path is
     * still pending, its content is replaced by the newer snapshot.
     */
    public static CompletableFuture<Void> write(final Path path, final String content) {
        final Path key = path.toAbsolutePath().normalize();
        final PendingWrite existing = PENDING.get(key);
        if (existing != null && existing.replace(content)) {
            return existing.future;
        }
        final PendingWrite pending = new PendingWrite(content);
        PENDING.put(key, pending);
        QUEUE_DEPTH.incrementAndGet();
        executor().execute(() -> {
            try {
                PENDING.remove(key, pending);
                final String snapshot = pending.take();
                writeAtomically(key, snapshot);
                pending.future.complete(null);
            } catch (final Throwable t) {
                LOGGER.error("Failed to write {}", key, t);
                pending.future.completeExceptionally(t);
            } finally {
                QUEUE_DEPTH.decrementAndGet();
            }
        });
        return pending.future;
    }

    public static void writeBlocking(final Path path, final String content) throws IOException {
        writeAtomically(path.toAbsolutePath().normalize(), content);
    }

    static void writeAtomically(final Path path, final String content) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final Path temp = parent == null ? Path.of(path.getFileName() + ".tmp") : parent.resolve(path.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException ex) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Drain pending writes; used on server shutdown (Section 5.2).
     */
    public static synchronized boolean shutdown(final long timeout, final TimeUnit unit) {
        final ExecutorService exec = EXECUTOR;
        if (exec == null) {
            return true;
        }
        exec.shutdown();
        try {
            final boolean done = exec.awaitTermination(timeout, unit);
            if (!done) {
                LOGGER.error("Essentials could not drain {} pending data writes before the deadline!", QUEUE_DEPTH.get());
            }
            return done;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            EXECUTOR = null;
        }
    }

    private static final class PendingWrite {
        private final CompletableFuture<Void> future = new CompletableFuture<>();
        private String content;
        private boolean taken;

        PendingWrite(final String content) {
            this.content = content;
        }

        synchronized boolean replace(final String newContent) {
            if (taken) {
                return false;
            }
            this.content = newContent;
            return true;
        }

        synchronized String take() {
            taken = true;
            return content;
        }
    }
}
