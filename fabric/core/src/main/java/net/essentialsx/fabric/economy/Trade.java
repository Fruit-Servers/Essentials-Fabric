package net.essentialsx.fabric.economy;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.SetExpFix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A charge or payment consisting of money, an item stack, experience or a configured
 * command cost (Section 7.3 / 10.4).
 */
public class Trade {
    private static Writer fw = null;
    private final transient String command;
    private final transient Trade fallbackTrade;
    private final transient BigDecimal money;
    private final transient ItemStack itemStack;
    private final transient Integer exp;
    private final transient Essentials ess;

    public Trade(final String command, final Essentials ess) {
        this(command, null, null, null, null, ess);
    }

    public Trade(final String command, final Trade fallback, final Essentials ess) {
        this(command, fallback, null, null, null, ess);
    }

    public Trade(final BigDecimal money, final Essentials ess) {
        this(null, null, money, null, null, ess);
    }

    public Trade(final ItemStack items, final Essentials ess) {
        this(null, null, null, items, null, ess);
    }

    public Trade(final int exp, final Essentials ess) {
        this(null, null, null, null, exp, ess);
    }

    private Trade(final String command, final Trade fallback, final BigDecimal money, final ItemStack item, final Integer exp, final Essentials ess) {
        this.command = command;
        this.fallbackTrade = fallback;
        this.money = money;
        this.itemStack = item;
        this.exp = exp;
        this.ess = ess;
    }

    public static void log(final String type, final String subtype, final String event, final String sender, final Trade charge, final String receiver, final Trade pay, final LazyLocation loc, final BigDecimal endBalance, final Essentials ess) {
        if ((loc == null && !ess.getSettings().isEcoLogUpdateEnabled()) || (loc != null && !ess.getSettings().isEcoLogEnabled())) {
            return;
        }
        synchronized (Trade.class) {
            if (fw == null) {
                try {
                    final Path logs = ess.getDataFolder().resolve("logs");
                    Files.createDirectories(logs);
                    fw = Files.newBufferedWriter(logs.resolve("economy.log"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (final IOException ex) {
                    ess.getLogger().error("Could not open economy log", ex);
                    return;
                }
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(type).append(",").append(subtype).append(",").append(event).append(",\"");
            sb.append(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date()));
            sb.append("\",\"");
            if (sender != null) {
                String senderIdentifier = sender;
                if (ess.getSettings().isEcoLogUUIDEnabled()) {
                    final UUID uuid = ess.getUsers().getUuid(sender);
                    if (uuid != null) {
                        senderIdentifier = uuid.toString();
                    }
                }
                sb.append(senderIdentifier);
            }
            sb.append("\",");
            appendTrade(sb, charge, ess);
            sb.append(",\"");
            if (receiver != null) {
                String receiverIdentifier = receiver;
                if (ess.getSettings().isEcoLogUUIDEnabled()) {
                    final UUID uuid = ess.getUsers().getUuid(receiver);
                    if (uuid != null) {
                        receiverIdentifier = uuid.toString();
                    }
                }
                sb.append(receiverIdentifier);
            }
            sb.append("\",");
            appendTrade(sb, pay, ess);
            if (loc == null) {
                sb.append(",\"\",\"\",\"\",\"\"");
            } else {
                sb.append(",\"");
                sb.append(loc.worldDisplayName(ess.getServer())).append("\",");
                sb.append(loc.blockX()).append(",");
                sb.append(loc.blockY()).append(",");
                sb.append(loc.blockZ()).append(",");
            }
            if (endBalance == null) {
                sb.append(",");
            } else {
                sb.append(endBalance);
                sb.append(",");
            }
            sb.append("\n");
            try {
                fw.write(sb.toString());
                fw.flush();
            } catch (final IOException ex) {
                ess.getLogger().error("Could not write economy log", ex);
            }
        }
    }

    private static void appendTrade(final StringBuilder sb, final Trade trade, final Essentials ess) {
        if (trade == null) {
            sb.append("\"\",\"\",\"\"");
            return;
        }
        if (trade.getItemStack() != null) {
            sb.append(trade.getItemStack().getCount()).append(",");
            sb.append(net.essentialsx.fabric.user.UserData.itemKey(trade.getItemStack())).append(",");
        }
        if (trade.getMoney() != null) {
            sb.append(trade.getMoney()).append(",");
            sb.append("money").append(",");
            sb.append(ess.getSettings().getCurrencySymbol());
        }
        if (trade.getExperience() != null) {
            sb.append(trade.getExperience()).append(",");
            sb.append("exp").append(",");
            sb.append("\"\"");
        }
    }

    public static synchronized void closeLog() {
        if (fw != null) {
            try {
                fw.close();
            } catch (final IOException ignored) {
            }
            fw = null;
        }
    }

    public void isAffordableFor(final IUser user) throws ChargeException {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        isAffordableFor(user, future);
        if (future.isCompletedExceptionally()) {
            try {
                future.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw (ChargeException) e.getCause();
            }
        }
    }

    public void isAffordableFor(final IUser user, final CompletableFuture<Boolean> future) {
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("checking if " + user.getName() + " can afford charge.");
        }
        if (getMoney() != null && getMoney().signum() > 0 && !user.canAfford(getMoney())) {
            future.completeExceptionally(new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(getMoney(), ess))));
            return;
        }
        final ServerPlayer base = user.getBase();
        if (getItemStack() != null && (base == null || !Inventories.containsAtLeast(base, itemStack, itemStack.getCount()))) {
            future.completeExceptionally(new ChargeException("missingItems", getItemStack().getCount(), ess.getItemDb().name(getItemStack())));
            return;
        }
        final BigDecimal money;
        if (command != null && !command.isEmpty() && (money = getCommandCost(user)).signum() > 0 && !user.canAfford(money)) {
            future.completeExceptionally(new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(money, ess))));
            return;
        }
        if (exp != null && exp > 0 && (base == null || SetExpFix.getTotalExperience(base) < exp)) {
            future.completeExceptionally(new ChargeException("notEnoughExperience"));
        }
    }

    public boolean pay(final IUser user) throws Exception {
        return pay(user, OverflowType.ABORT) == null;
    }

    public Map<Integer, ItemStack> pay(final IUser user, final OverflowType type) throws Exception {
        if (getMoney() != null && getMoney().signum() > 0) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("paying user " + user.getName() + " via trade " + getMoney().toPlainString());
            }
            user.giveMoney(getMoney());
        }
        final ServerPlayer base = user.getBase();
        if (getItemStack() != null && base != null) {
            if (type == OverflowType.ABORT && !Inventories.hasSpace(base, 0, false, getItemStack())) {
                return Collections.singletonMap(0, getItemStack());
            }
            final Map<Integer, ItemStack> leftover = Inventories.addItem(base, getItemStack());
            if (!leftover.isEmpty()) {
                if (type == OverflowType.RETURN) {
                    return leftover;
                } else {
                    for (final ItemStack stack : leftover.values()) {
                        Inventories.dropNaturally(base, stack);
                    }
                }
            }
        }
        if (getExperience() != null && base != null) {
            SetExpFix.setTotalExperience(base, SetExpFix.getTotalExperience(base) + getExperience());
        }
        return null;
    }

    public void charge(final IUser user) throws ChargeException {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        charge(user, future);
        if (future.isCompletedExceptionally()) {
            try {
                future.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw (ChargeException) e.getCause();
            }
        }
    }

    public void charge(final IUser user, final CompletableFuture<Boolean> future) {
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("attempting to charge user " + user.getName());
        }
        if (getMoney() != null) {
            if (!user.canAfford(getMoney()) && getMoney().signum() > 0) {
                future.completeExceptionally(new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(getMoney(), ess))));
                return;
            }
            user.takeMoney(getMoney());
        }
        final ServerPlayer base = user.getBase();
        if (getItemStack() != null) {
            if (base == null || !Inventories.containsAtLeast(base, getItemStack(), getItemStack().getCount())) {
                future.completeExceptionally(new ChargeException("missingItems", getItemStack().getCount(), net.essentialsx.fabric.user.UserData.itemKey(getItemStack()).toLowerCase(Locale.ENGLISH).replace("_", " ")));
                return;
            }
            Inventories.removeItemAmount(base, getItemStack(), getItemStack().getCount());
        }
        if (command != null) {
            final BigDecimal cost = getCommandCost(user);
            if (!user.canAfford(cost) && cost.signum() > 0) {
                future.completeExceptionally(new ChargeException("notEnoughMoney", Text.parsed(NumberUtil.displayCurrency(cost, ess))));
                return;
            }
            user.takeMoney(cost);
        }
        if (getExperience() != null) {
            final int experience = base == null ? 0 : SetExpFix.getTotalExperience(base);
            if (experience < getExperience() && getExperience() > 0) {
                future.completeExceptionally(new ChargeException("notEnoughExperience"));
                return;
            }
            if (base != null) {
                SetExpFix.setTotalExperience(base, experience - getExperience());
            }
        }
    }

    public BigDecimal getMoney() {
        return money;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Integer getExperience() {
        return exp;
    }

    public TradeType getType() {
        if (getExperience() != null) {
            return TradeType.EXP;
        }
        if (getItemStack() != null) {
            return TradeType.ITEM;
        }
        return TradeType.MONEY;
    }

    public BigDecimal getCommandCost(final IUser user) {
        BigDecimal cost = BigDecimal.ZERO;
        if (command != null && !command.isEmpty()) {
            cost = ess.getSettings().getCommandCost(command.charAt(0) == '/' ? command.substring(1) : command);
            if (cost.signum() == 0 && fallbackTrade != null) {
                cost = fallbackTrade.getCommandCost(user);
            }
        }
        if (cost.signum() != 0 && (user.isAuthorized("essentials.nocommandcost.all") || user.isAuthorized("essentials.nocommandcost." + command))) {
            return BigDecimal.ZERO;
        }
        return cost;
    }

    public enum TradeType {
        MONEY,
        EXP,
        ITEM
    }

    public enum OverflowType {
        ABORT,
        DROP,
        RETURN
    }
}
