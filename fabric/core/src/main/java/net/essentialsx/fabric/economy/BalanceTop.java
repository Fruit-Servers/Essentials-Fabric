package net.essentialsx.fabric.economy;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Cached balance ranking built asynchronously from Impactor accounts (Section 10.5).
 */
public class BalanceTop {
    private final Essentials ess;
    private LinkedHashMap<UUID, Entry> topCache = new LinkedHashMap<>();
    private BigDecimal cacheTotal = BigDecimal.ZERO;
    private long cacheAge = 0L;
    private CompletableFuture<Void> cacheLock;

    public BalanceTop(final Essentials ess) {
        this.ess = ess;
    }

    public record Entry(UUID uuid, String name, BigDecimal balance) {
    }

    public CompletableFuture<Void> calculateBalanceTopMapAsync() {
        if (cacheLock != null && !cacheLock.isDone()) {
            return cacheLock;
        }
        final CompletableFuture<Void> future = new CompletableFuture<>();
        cacheLock = future;
        if (ess.getEconomy() == null) {
            future.complete(null);
            return future;
        }
        ess.getEconomy().top().whenComplete((ranked, throwable) -> {
            if (throwable != null) {
                ess.getLogger().error("Failed to build balance top", throwable);
                future.completeExceptionally(throwable);
                return;
            }
            final List<ImpactorEconomy.RankedBalance> list = ranked;
            ess.getServer().execute(() -> {
                try {
                    final LinkedHashMap<UUID, Entry> newTop = new LinkedHashMap<>();
                    BigDecimal newTotal = BigDecimal.ZERO;
                    final BigDecimal minBalance = ess.getSettings().getBaltopMinBalance();
                    final long minPlaytime = ess.getSettings().getBaltopMinPlaytime();
                    final int limit = ess.getSettings().getBaltopEntryLimit();
                    for (final ImpactorEconomy.RankedBalance rb : list) {
                        final User user = ess.getUser(rb.owner());
                        final String name = user != null ? user.getName() : ess.getUsers().getName(rb.owner());
                        if (name == null) {
                            continue;
                        }
                        if (user != null) {
                            if (user.isNPC() && !ess.getSettings().isNpcsInBalanceRanking()) {
                                continue;
                            }
                            if (user.isBaltopExempt()) {
                                continue;
                            }
                            if (minPlaytime > 0 && user.getPlaytimeTicksLive() / 20 < minPlaytime) {
                                continue;
                            }
                        }
                        if (rb.balance().compareTo(minBalance) < 0) {
                            continue;
                        }
                        if (!ess.getSettings().showZeroBaltop() && rb.balance().signum() == 0) {
                            continue;
                        }
                        newTotal = newTotal.add(rb.balance());
                        if (limit < 0 || newTop.size() < limit) {
                            newTop.put(rb.owner(), new Entry(rb.owner(), name, rb.balance()));
                        }
                    }
                    topCache = newTop;
                    cacheTotal = newTotal;
                    cacheAge = System.currentTimeMillis();
                    future.complete(null);
                } catch (final Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        });
        return future;
    }

    public Map<UUID, Entry> getBalanceTopCache() {
        return topCache;
    }

    public long getCacheAge() {
        return cacheAge;
    }

    public BigDecimal getBalanceTopTotal() {
        return cacheTotal;
    }

    public boolean isCacheLocked() {
        return cacheLock != null && !cacheLock.isDone();
    }

    public void invalidate() {
        cacheAge = 0L;
    }
}
