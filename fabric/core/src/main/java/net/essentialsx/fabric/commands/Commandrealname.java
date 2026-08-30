package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Locale;

public class Commandrealname extends EssentialsCommand {
    public Commandrealname() {
        super("realname");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final String lookup = args[0].toLowerCase(Locale.ENGLISH);
        final boolean skipHidden = sender.isPlayer() && !ess.getUser(sender.getPlayer()).canInteractVanished();
        boolean foundUser = false;
        for (final User u : ess.getOnlineUsers()) {
            if (skipHidden && u.isHidden(sender.getPlayer()) && u.isHiddenFrom(sender.getPlayer())) {
                continue;
            }
            u.setDisplayNick();
            if (FormatUtil.stripFormat(u.getDisplayName()).toLowerCase(Locale.ENGLISH).contains(lookup)) {
                foundUser = true;
                sender.sendTl("realName", u.getDisplayName(), u.getName());
            }
        }
        if (!foundUser) {
            throw new PlayerNotFoundException();
        }
    }
}
