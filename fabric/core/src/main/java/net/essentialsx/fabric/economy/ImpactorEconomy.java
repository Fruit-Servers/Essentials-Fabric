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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Essentials policy over the Impactor 5.3.5 economy API (Section 10).
 *
 * <p>Impactor owns accounts, currencies and durability. This adapter binds one currency
 * for the server lifetime, resolves accounts asynchronously and preserves Impactor
 * transaction results. It deliberately holds no {@code Account} objects of its own: Impactor
 * evicts accounts from its cache after a short idle period, and any copy kept here would go
 * stale as soon as another mod (GTS, shops, crates, ...) touched the same account through a
 * freshly resolved instance. Every call goes through {@code service.account(currency, uuid)}
 * so Essentials always sees the same account state as everyone else.
 */
public class ImpactorEconomy {
    private final Essentials ess;
    private final EconomyService service;
    private final Currency currency;

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
        return service.account(currency, uuid);
    }

    /**
     * Account from Impactor's own cache when it can be resolved without waiting, or null
     * when Impactor would have to hit storage. Server-thread paths use this and fall back
     * to a bounded async wait when it returns null.
     */
    public Account cachedAccount(final UUID uuid) {
        final CompletableFuture<Account> future = service.account(currency, uuid);
        return future.isDone() && !future.isCompletedExceptionally() ? future.join() : null;
    }

    public CompletableFuture<Boolean> hasAccount(final UUID uuid) {
        return service.hasAccount(currency, uuid);
    }

    /**
     * Warm Impactor's account cache (for example on join) so the first server-thread
     * lookups resolve without touching storage.
     */
    public void preload(final UUID uuid) {
        account(uuid).exceptionally(t -> {
            ess.getLogger().warn("Failed to load Impactor account for {}: {}", uuid, t.getMessage());
            return null;
        });
    }

    // ------------------------------------------------------------ balances

    public CompletableFuture<BigDecimal> balance(final UUID uuid) {
        return account(uuid).thenApply(Account::balance);
    }

    /**
     * Balance when Impactor can resolve the account immediately, otherwise null so callers
     * can fall back to the user's imported/cached value.
     */
    public BigDecimal balanceNow(final UUID uuid) {
        final Account account = cachedAccount(uuid);
        return account != null ? account.balance() : null;
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

    // synchronous variants operating on an already-resolved account (server thread)

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
