package net.essentialsx.fabric.commands;

import com.mojang.brigadier.tree.CommandNode;
import net.essentialsx.fabric.command.CommandRegistry;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.textreader.HelpInput;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.TextInput;
import net.essentialsx.fabric.textreader.TextPager;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public class Commandhelp extends EssentialsCommand {
    public Commandhelp() {
        super("help");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final IText output;
        String pageStr = args.length > 0 ? args[0] : null;
        String chapterPageStr = args.length > 1 ? args[1] : null;
        String command = commandLabel;
        final IText input = new TextInput(user.getSource(), "help", false, ess);
        if (input.getLines().isEmpty()) {
            if (pageStr != null && pageStr.startsWith("/")) {
                final String cmd = pageStr.substring(1);
                if (showCommandHelp(server, user, cmd)) {
                    return;
                }
            }
            if (NumberUtil.isInt(pageStr) || pageStr == null) {
                output = new HelpInput(user, "", ess);
            } else {
                if (pageStr.length() > 26) {
                    pageStr = pageStr.substring(0, 25);
                }
                output = new HelpInput(user, pageStr.toLowerCase(Locale.ENGLISH), ess);
                command = command.concat(" ").concat(pageStr);
                pageStr = chapterPageStr;
            }
            chapterPageStr = null;
        } else {
            user.setDisplayNick();
            output = new KeywordReplacer(input, user.getSource(), ess);
        }
        final TextPager pager = new TextPager(output);
        pager.showPage(pageStr, chapterPageStr, command, user.getSource());
    }

    private boolean showCommandHelp(final MinecraftServer server, final User user, final String cmd) {
        final CommandRegistry registry = ess.getCommandRegistry();
        final String resolved = registry.resolve(cmd);
        if (resolved != null) {
            final CommandRegistry.CommandInfo info = registry.getInfo(resolved);
            final EssentialsCommand essCommand = registry.getCommand(resolved);
            user.sendTl("commandHelpLine1", cmd);
            String description = info != null ? info.description() : "";
            try {
                description = user.playerTl(resolved + "CommandDescription");
            } catch (final MissingResourceException ignored) {
            }
            user.sendTl("commandHelpLine2", description);
            user.sendTl("commandHelpLine4", info != null ? info.aliases().toString() : "[]");
            user.sendTl("commandHelpLine3");
            if (essCommand != null && !essCommand.getUsageStrings().isEmpty()) {
                for (final Map.Entry<String, String> usage : essCommand.getUsageStrings().entrySet()) {
                    user.sendTl("commandHelpLineUsage", Text.parsed(usage.getKey().replace("<command>", cmd)), Text.parsed(user.playerTl(usage.getValue())));
                }
            } else if (info != null) {
                user.sendMessage(info.usage());
            }
            return true;
        }
        for (final CommandNode<CommandSourceStack> node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            if (node.getName().equalsIgnoreCase(cmd)) {
                user.sendTl("commandHelpLine1", cmd);
                user.sendTl("commandHelpLine2", "");
                user.sendTl("commandHelpLine4", "[]");
                user.sendTl("commandHelpLine3");
                for (final String usage : server.getCommands().getDispatcher().getSmartUsage(node, user.getBase().createCommandSourceStack()).values()) {
                    user.sendMessage("/" + cmd + " " + usage);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendTl("helpConsole");
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> suggestions = new ArrayList<>(getCommands(server));
            suggestions.addAll(getMods());
            return suggestions;
        } else {
            return Collections.emptyList();
        }
    }
}
