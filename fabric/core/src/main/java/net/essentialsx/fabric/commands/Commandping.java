package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

// This command can be used to echo messages to the users screen, mostly useless but also an #EasterEgg
public class Commandping extends EssentialsCommand {
    public Commandping() {
        super("ping");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            sender.sendTl("pong");
        } else {
            sender.sendMessage(FormatUtil.replaceFormat(getFinalArg(args, 0)));
        }
    }
}
