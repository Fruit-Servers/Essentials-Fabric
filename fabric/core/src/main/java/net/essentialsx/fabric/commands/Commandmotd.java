package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.TextInput;
import net.essentialsx.fabric.textreader.TextPager;
import net.minecraft.server.MinecraftServer;

public class Commandmotd extends EssentialsCommand {
    public Commandmotd() {
        super("motd");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (sender.isPlayer()) {
            ess.getUser(sender.getPlayer()).setDisplayNick();
        }
        final TextPager pager = new TextPager(new KeywordReplacer(new TextInput(sender, "motd", true, ess), sender, ess));
        pager.showPage(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null, commandLabel, sender);
    }
}
