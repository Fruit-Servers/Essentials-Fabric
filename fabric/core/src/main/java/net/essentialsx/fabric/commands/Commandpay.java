package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Commandpay extends EssentialsLoopCommand {
    public Commandpay() {
        super("pay");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final String ogStr = args[1];
        if (ogStr.contains("-")) {
            throw new TranslatableException("payMustBePositive");
        }
        final String sanitizedString = ogStr.replaceAll("[^0-9.]", "");
        if (sanitizedString.isEmpty()) {
            throw new NotEnoughArgumentsException();
        }
        final BigDecimal amount;
        if (ess.getSettings().isPerPlayerLocale()) {
            amount = NumberUtil.parseStringToBDecimal(ogStr, user.getPlayerLocale(user.getBase().clientInformation().language()));
        } else {
            amount = NumberUtil.parseStringToBDecimal(ogStr);
        }
        if (amount.compareTo(ess.getSettings().getMinimumPayAmount()) < 0) {
            throw new TranslatableException("minimumPayAmount", Text.parsed(NumberUtil.displayCurrencyExactly(ess.getSettings().getMinimumPayAmount(), ess)));
        }
        final AtomicBoolean informToConfirm = new AtomicBoolean(false);
        final boolean canPayOffline = user.isAuthorized("essentials.pay.offline");
        if (!canPayOffline && args[0].equals("**")) {
            user.sendTl("payOffline");
            return;
        }
        loopOfflinePlayersConsumer(server, user.getSource(), false, user.isAuthorized("essentials.pay.multiple"), args[0], player -> {
            try {
                if ((!player.isOnline() || player.isHidden(user.getBase())) && !canPayOffline) {
                    user.sendTl("payOffline");
                    return;
                }
                if (!player.isAcceptingPay() || (ess.getSettings().isPayExcludesIgnoreList() && player.isIgnoredPlayer(user))) {
                    user.sendTl("notAcceptingPay", player.getDisplayName());
                    return;
                }
                if (user.isPromptingPayConfirm() && !amount.equals(user.getConfirmingPayments().get(player))) {
                    if (!informToConfirm.get()) {
                        user.getConfirmingPayments().clear();
                        informToConfirm.set(true);
                    }
                    user.getConfirmingPayments().put(player, amount);
                    return;
                }
                user.payUser(player, amount);
                user.getConfirmingPayments().remove(player);
                Trade.log("Command", "Pay", "Player", user.getName(), new Trade(amount, ess), player.getName(), new Trade(amount, ess), user.getLocation(), user.getMoney(), ess);
            } catch (final MaxMoneyException ex) {
                user.sendTl("maxMoney");
                try {
                    user.setMoney(user.getMoney().add(amount));
                } catch (final Exception ignored) {
                }
            } catch (final TranslatableException e) {
                throw e;
            } catch (final Exception e) {
                throw new TranslatableException("errorWithMessage", e.getMessage());
            }
        });
        if (informToConfirm.get()) {
            final String cmd = "/" + commandLabel + " " + StringUtil.joinList(" ", (Object[]) args);
            user.sendTl("confirmPayment", Text.parsed(NumberUtil.displayCurrency(amount, ess)), cmd);
        }
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User player, final String[] args) {
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            return new ArrayList<>(List.of(ess.getSettings().getMinimumPayAmount().toString()));
        } else {
            return Collections.emptyList();
        }
    }
}
