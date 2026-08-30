package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerExemptException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandeco extends EssentialsLoopCommand {
    public Commandeco() {
        super("eco");
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final EcoCommands cmd;
        final boolean isPercent;
        final BigDecimal amount;
        try {
            cmd = EcoCommands.valueOf(args[0].toUpperCase(Locale.ENGLISH));
            isPercent = cmd != EcoCommands.RESET && args[2].endsWith("%");
            if (cmd == EcoCommands.RESET) {
                amount = ess.getSettings().getStartingBalance();
            } else if (sender.isPlayer() && ess.getSettings().isPerPlayerLocale()) {
                amount = NumberUtil.parseStringToBDecimal(args[2], ess.getUser(sender.getPlayer()).getPlayerLocale(sender.getPlayer().clientInformation().language()));
            } else {
                amount = NumberUtil.parseStringToBDecimal(args[2]);
            }
        } catch (final Exception ex) {
            throw new NotEnoughArgumentsException(ex);
        }
        loopOfflinePlayersConsumer(server, sender, false, true, args[1], player -> {
            BigDecimal userAmount = amount;
            if (isPercent) {
                userAmount = player.getMoney().multiply(userAmount).scaleByPowerOfTen(-2);
            }
            try {
                switch (cmd) {
                    case GIVE: {
                        player.giveMoney(userAmount, sender);
                        break;
                    }
                    case TAKE: {
                        if (player.getMoney().subtract(userAmount).compareTo(ess.getSettings().getMinMoney()) >= 0) {
                            player.takeMoney(userAmount, sender);
                        } else {
                            ess.showError(sender, new TranslatableException("minimumBalanceError", Text.parsed(NumberUtil.displayCurrency(ess.getSettings().getMinMoney(), ess))), commandLabel);
                        }
                        break;
                    }
                    case RESET:
                    case SET: {
                        final BigDecimal minBal = ess.getSettings().getMinMoney();
                        final BigDecimal maxBal = ess.getSettings().getMaxMoney();
                        final boolean underMin = userAmount.compareTo(minBal) < 0;
                        final boolean aboveMax = userAmount.compareTo(maxBal) > 0;
                        if (cmd == EcoCommands.RESET && !ess.getSettings().isEcoResetUsesStartingBalance() && ess.getEconomy() != null) {
                            // Section 10.2: Impactor's currency starting balance stays authoritative on reset.
                            final var account = ess.getEconomy().account(player.getUUID()).join();
                            account.reset();
                            player.updateMoneyCache(account.balance());
                        } else {
                            player.setMoney(underMin ? minBal : aboveMax ? maxBal : userAmount);
                        }
                        player.sendTl("setBal", Text.parsed(NumberUtil.displayCurrency(player.getMoney(), ess)));
                        sender.sendTl("setBalOthers", player.getDisplayName(), Text.parsed(NumberUtil.displayCurrency(player.getMoney(), ess)));
                        break;
                    }
                }
            } catch (final TranslatableException e) {
                throw e;
            } catch (final Exception e) {
                throw new TranslatableException(e, "errorWithMessage", e.getMessage());
            }
        });
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) throws NotEnoughArgumentsException, PlayerExemptException, ChargeException, MaxMoneyException {
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>();
            for (final EcoCommands command : EcoCommands.values()) {
                options.add(command.name().toLowerCase(Locale.ENGLISH));
            }
            return options;
        } else if (args.length == 2) {
            return getPlayers(sender);
        } else if (args.length == 3 && !args[0].equalsIgnoreCase(EcoCommands.RESET.name())) {
            if (args[0].equalsIgnoreCase(EcoCommands.SET.name())) {
                return new ArrayList<>(List.of("0", ess.getSettings().getStartingBalance().toString()));
            } else {
                return new ArrayList<>(List.of("1", "10", "100", "1000"));
            }
        } else {
            return Collections.emptyList();
        }
    }

    private enum EcoCommands {
        GIVE, TAKE, SET, RESET
    }
}
