package net.essentialsx.fabric.economy;

import net.essentialsx.fabric.Essentials;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.impactdev.impactor.api.economy.transactions.EconomyTransferTransaction;
import net.impactdev.impactor.api.economy.transactions.details.EconomyResultType;
import net.kyori.adventure.key.Key;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Essentials policy over the Impactor 5.3.5 economy API (Section 10).
 *
 * <p>Impactor owns accounts, currencies and durability. This adapter binds one currency
 * for the server lifetime, resolves accounts asynchronously, preserves Impactor
 * transaction results, and keeps a short-lived balance cache so command flows that
 * historically read synchronously (cost checks, sell, signs) can run on the server thread
 * without blocking on storage.
 */
public class ImpactorEconomy {
    private final Essentials ess;
    private final EconomyService service;
    private final Currency currency;
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> accountTimes = new ConcurrentHashMap<>();
    private static final long ACCOUNT_TTL = TimeUnit.MINUTES.toMillis(10);

    private ImpactorEconomy(final Essentials ess, final EconomyService service, final Currency currency) {
        this.ess = ess;
        this.service = service;
        this.currency = currency;
    }

    /**
     * Resolve the Impactor service and bind the configured currency (Section 10.2).
     *
     * @throws IllegalStateException when Impactor is absent, exposes no service or the
     *                               configured currency is unknown.
     */
    public static ImpactorEconomy bind(final Essentials ess) {
        final EconomyService service;
        try {
            service = EconomyService.instance();
        } catch (final Throwable t) {
            throw new IllegalStateException("Impactor 5.3.5+ is required but its EconomyService is unavailable: " + t.getMessage(), t);
        }
        if (service == null) {
            throw new IllegalStateException("Impactor exposes no EconomyService");
        }
        final String configured = ess.getSettings().getImpactorCurrency();
        final Currency currency;
        if (configured == null || configured.isBlank() || configured.equalsIgnoreCase("primary")) {
            currency = service.currencies().primary();
        } else {
            final Key key;
            try {
                key = configured.contains(":") ? Key.key(configured) : Key.key("impactor", configured.toLowerCase(Locale.ROOT));
            } catch (final Exception e) {
                throw new IllegalStateException("Invalid Impactor currency identifier '" + configured + "'", e);
            }
            final Optional<Currency> resolved = service.currencies().currency(key);
            if (resolved.isEmpty()) {
                throw new IllegalStateException("Unknown Impactor currency '" + configured + "'. Registered: " + service.currencies().registered().stream().map(c -> c.key().asString()).toList());
            }
            currency = resolved.get();
        }
        if (currency == null) {
            throw new IllegalStateException("Impactor has no primary/configured currency");
        }
        return new ImpactorEconomy(ess, service, currency);
    }

    public EconomyService service() {
        return service;
    }

    public Currency currency() {
        return currency;
    }

    public String currencyName() {
        return currency.key().asString();
    }

    /**
     * Normalise an amount to the bound currency's precision (Section 10.2).
     */
    public BigDecimal normalize(final BigDecimal amount) {
        return amount.setScale(Math.max(0, currency.decimals()), RoundingMode.DOWN);
    }

    // ------------------------------------------------------------ accounts

    public CompletableFuture<Account> account(final UUID uuid) {
        final Account cached = accounts.get(uuid);
        if (cached != null) {
            accountTimes.put(uuid, System.currentTimeMillis());
            return CompletableFuture.completedFuture(cached);
        }
        return service.account(currency, uuid).thenApply(account -> {
            accounts.put(uuid, account);
            accountTimes.put(uuid, System.currentTimeMillis());
            return account;
        });
    }

    /**
     * Cached account, or null when it has not been loaded yet. Online players are preloaded
     * on join so the server-thread command paths can use this safely.
     */
    public Account cachedAccount(final UUID uuid) {
        return accounts.get(uuid);
    }

    public CompletableFuture<Boolean> hasAccount(final UUID uuid) {
        if (accounts.containsKey(uuid)) {
            return CompletableFuture.completedFuture(true);
        }
        return service.hasAccount(currency, uuid);
    }

    public void preload(final UUID uuid) {
        account(uuid).exceptionally(t -> {
            ess.getLogger().warn("Failed to load Impactor account for {}: {}", uuid, t.getMessage());
            return null;
        });
    }

    public void unload(final UUID uuid) {
        accounts.remove(uuid);
        accountTimes.remove(uuid);
    }

    public void cleanupCache() {
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, Long> entry : accountTimes.entrySet()) {
            if (now - entry.getValue() > ACCOUNT_TTL && (ess.getServer() == null || ess.getServer().getPlayerList().getPlayer(entry.getKey()) == null)) {
                accounts.remove(entry.getKey());
                accountTimes.remove(entry.getKey());
            }
        }
    }

    // ------------------------------------------------------------ balances

    public CompletableFuture<BigDecimal> balance(final UUID uuid) {
        return account(uuid).thenApply(Account::balance);
    }

    /**
     * Balance from the cached account; falls back to the user's imported/cached value.
     */
    public BigDecimal balanceNow(final UUID uuid) {
        final Account account = accounts.get(uuid);
        if (account != null) {
            return account.balance();
        }
        return null;
    }

    public CompletableFuture<EconomyTransaction> set(final UUID uuid, final BigDecimal amount) {
        return account(uuid).thenApply(account -> account.set(normalize(amount)));
    }

    public CompletableFuture<EconomyTransaction> deposit(final UUID uuid, final BigDecimal amount) {
        return account(uuid).thenApply(account -> account.deposit(normalize(amount)));
    }

    public CompletableFuture<EconomyTransaction> withdraw(final UUID uuid, final BigDecimal amount) {
        return account(uuid).thenApply(account -> account.withdraw(normalize(amount)));
    }

    public CompletableFuture<EconomyTransaction> reset(final UUID uuid) {
        return account(uuid).thenApply(Account::reset);
    }

    /**
     * Player-to-player movement uses Impactor's transfer transaction (Section 10.4).
     */
    public CompletableFuture<EconomyTransferTransaction> transfer(final UUID from, final UUID to, final BigDecimal amount) {
        return account(from).thenCombine(account(to), (a, b) -> a.transfer(b, normalize(amount)));
    }

    // synchronous variants operating on cached accounts (server thread, preloaded users)

    public EconomyTransaction setNow(final Account account, final BigDecimal amount) {
        return account.set(normalize(amount));
    }

    public EconomyTransaction depositNow(final Account account, final BigDecimal amount) {
        return account.deposit(normalize(amount));
    }

    public EconomyTransaction withdrawNow(final Account account, final BigDecimal amount) {
        return account.withdraw(normalize(amount));
    }

    // ------------------------------------------------------------ ranking

    public CompletableFuture<List<RankedBalance>> top() {
        return service.accounts(currency).thenApply(collection -> {
            final Collection<Account> all = collection == null ? List.of() : collection;
            final List<RankedBalance> result = new ArrayList<>();
            for (final Account account : all) {
                if (account.virtual()) {
                    continue;
                }
                result.add(new RankedBalance(account.owner(), account.balance()));
            }
            result.sort(Comparator.comparing(RankedBalance::balance).reversed());
            return result;
        });
    }

    public static boolean success(final EconomyTransaction tx) {
        return tx != null && tx.result() == EconomyResultType.SUCCESS;
    }

    public static boolean success(final EconomyTransferTransaction tx) {
        return tx != null && tx.result() == EconomyResultType.SUCCESS;
    }

    public record RankedBalance(UUID owner, BigDecimal balance) {
    }
}
