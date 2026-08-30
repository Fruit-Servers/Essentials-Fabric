package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandbalance extends EssentialsCommand {
    public Commandbalance() {
        super("balance");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final User target = getPlayer(server, args, 0, true, true);
        sender.sendTl("balanceOther", target.isHidden() ? target.getName() : target.getDisplayName(), Text.parsed(NumberUtil.displayCurrency(target.getMoney(), ess)));
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 1 && user.isAuthorized("essentials.balance.others")) {
            final User target = getPlayer(server, args, 0, false, true);
            user.sendTl("balanceOther", target.isHidden() ? target.getName() : target.getDisplayName(), Text.parsed(NumberUtil.displayCurrency(target.getMoney(), ess)));
        } else if (args.length < 2) {
            user.sendTl("balance", Text.parsed(NumberUtil.displayCurrency(user.getMoney(), ess)));
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.balance.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
