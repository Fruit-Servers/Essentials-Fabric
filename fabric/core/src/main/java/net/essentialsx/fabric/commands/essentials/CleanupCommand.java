package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.NumberUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CleanupCommand extends EssentialsTreeNode {
    public CleanupCommand() {
        super("cleanup");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1 || !NumberUtil.isInt(args[0])) {
            sender.sendMessage("This sub-command will delete users who haven't logged in in the last <days> days.");
            sender.sendMessage("Optional parameters define the minimum amount required to prevent deletion.");
            sender.sendMessage("Unless you define larger default values, this command will ignore people who have more than 0 money/homes.");
            throw new Exception("/" + commandLabel + " cleanup <days> [money] [homes]");
        }
        sender.sendTl("cleaning");
        final long daysArg = Long.parseLong(args[0]);
        final double moneyArg = args.length >= 2 ? Double.parseDouble(args[1].replaceAll("[^0-9.]", "")) : 0;
        final int homesArg = args.length >= 3 && NumberUtil.isInt(args[2]) ? Integer.parseInt(args[2]) : 0;
        ess.runTaskAsynchronously(() -> {
            final long currTime = System.currentTimeMillis();
            for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                final User user = ess.getUsers().getUser(u);
                if (user == null) {
                    continue;
                }
                long lastLog = user.getLastLogout();
                if (lastLog == 0) {
                    lastLog = user.getLastLogin();
                }
                if (lastLog == 0) {
                    user.setLastLogin(currTime, user.getLastLoginAddress());
                }
                if (user.isNPC()) {
                    continue;
                }
                final long timeDiff = currTime - lastLog;
                final long milliDays = daysArg * 24L * 60L * 60L * 1000L;
                final int homeCount = user.getHomes().size();
                final double moneyCount = user.getMoney().doubleValue();
                if (lastLog == 0 || timeDiff < milliDays || homeCount > homesArg || moneyCount > moneyArg) {
                    continue;
                }
                if (ess.getSettings().isDebug()) {
                    ess.getLogger().info("Deleting user: " + user.getName() + " Money: " + moneyCount + " Homes: " + homeCount + " Last seen: " + DateUtil.formatDateDiff(lastLog));
                }
                user.reset();
            }
            sender.sendTl("cleaned");
        });
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return EssentialsCommand.COMMON_DURATIONS;
        } else if (args.length == 2 || args.length == 3) {
            return new ArrayList<>(List.of("-1", "0"));
        }
        return Collections.emptyList();
    }
}
