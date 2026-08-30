package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.TextInput;
import net.essentialsx.fabric.textreader.TextPager;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

public class Commandcustomtext extends EssentialsCommand {
    public Commandcustomtext() {
        super("customtext");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (sender.isPlayer()) {
            ess.getUser(sender.getPlayer()).setDisplayNick();
        }
        final IText input = new TextInput(sender, "custom", true, ess);
        final IText output = new KeywordReplacer(input, sender, ess);
        final TextPager pager = new TextPager(output);
        String chapter = commandLabel;
        final String page;
        if (commandLabel.equalsIgnoreCase("customtext") && args.length > 0 && !NumberUtil.isInt(commandLabel)) {
            chapter = args[0];
            page = args.length > 1 ? args[1] : null;
        } else {
            page = args.length > 0 ? args[0] : null;
        }
        pager.showPage(chapter, page, null, sender);
    }
}
