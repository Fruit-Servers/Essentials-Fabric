package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

public class Commandsetjail extends EssentialsCommand {
    public Commandsetjail() {
        super("setjail");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        ess.getJails().setJail(args[0], user.getLocation());
        user.sendTl("jailSet", StringUtil.sanitizeString(args[0]));
    }
}
