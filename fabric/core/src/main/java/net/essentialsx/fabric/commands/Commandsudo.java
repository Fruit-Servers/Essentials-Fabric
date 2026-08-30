package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.listener.PlayerChat;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Locale;

public class Commandsudo extends EssentialsLoopCommand {
    public Commandsudo() {
        super("sudo");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final String command = getFinalArg(args, 1);
        final boolean multiple = !sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.sudo.multiple");
        sender.sendTl("sudoRun", args[0], command, "");
        loopOnlinePlayers(server, sender, false, multiple, args[0], new String[] {command});
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
        if (user.getName().equals(sender.getName())) {
            return; // Silently don't do anything.
        }
        if (user.isAuthorized("essentials.sudo.exempt") && sender.isPlayer()) {
            sender.sendTl("sudoExempt", user.getName());
            return;
        }
        if (args[0].toLowerCase(Locale.ENGLISH).startsWith("c:")) {
            PlayerChat.send(ess, user.getBase(), getFinalArg(args, 0).substring(2));
            return;
        }
        final String command = getFinalArg(args, 0);
        if (command.length() > 0) {
            ess.scheduleSyncDelayedTask(() -> {
                try {
                    ess.getCommandRegistry().dispatchAsUser(user, command);
                } catch (final Exception e) {
                    sender.sendTl("errorCallingCommand", command);
                }
            });
        }
    }
}
