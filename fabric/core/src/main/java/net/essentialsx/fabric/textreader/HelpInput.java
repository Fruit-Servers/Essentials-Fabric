package net.essentialsx.fabric.textreader;

import com.mojang.brigadier.tree.CommandNode;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandRegistry;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Builds the {@code /help} listing from registered Essentials commands and, optionally,
 * every other command on the Brigadier dispatcher the user may run.
 */
public class HelpInput implements IText {
    private final transient List<String> lines = new ArrayList<>();
    private final transient List<String> chapters = new ArrayList<>();
    private final transient Map<String, Integer> bookmarks = new HashMap<>();

    public HelpInput(final User user, final String match, final Essentials ess) {
        final List<String> newLines = new ArrayList<>();
        if (!match.equalsIgnoreCase("")) {
            lines.add(Text.get().miniToLegacy(user.playerTl("helpMatching", match)));
        }
        final CommandRegistry registry = ess.getCommandRegistry();
        final String pluginName = "Essentials";
        final String pluginNameLow = "essentials";
        if (pluginNameLow.equals(match)) {
            lines.clear();
            lines.add(Text.get().miniToLegacy(user.playerTl("helpFrom", pluginName)));
        }
        final List<String> essLines = new ArrayList<>();
        for (final String commandName : new TreeSet<>(registry.getCommandNames())) {
            final CommandRegistry.CommandInfo info = registry.getInfo(commandName);
            final String commandDescription = net.essentialsx.fabric.text.I18n.hasKey(commandName + "CommandDescription") ? Text.get().miniToLegacy(tlLiteral(commandName + "CommandDescription")) : (info == null ? "" : info.description());
            if (!match.equalsIgnoreCase("")
                && (!pluginNameLow.contains(match))
                && (!commandName.toLowerCase(Locale.ENGLISH).contains(match))
                && (!commandDescription.toLowerCase(Locale.ENGLISH).contains(match))) {
                continue;
            }
            final String node = "essentials." + (commandName.equals("r") ? "msg" : commandName);
            if (!ess.getSettings().isCommandDisabled(commandName) && user.isAuthorized(node)) {
                essLines.add(Text.get().miniToLegacy(user.playerTl("helpLine", commandName, commandDescription)));
            }
        }
        if (!essLines.isEmpty()) {
            newLines.addAll(essLines);
            if (match.equalsIgnoreCase("")) {
                lines.add(Text.get().miniToLegacy(user.playerTl("helpPlugin", pluginName, pluginNameLow)));
            }
        }
        if (!pluginNameLow.equals(match) && ess.getSettings().showNonEssCommandsInHelp() && user.getBase() != null) {
            final List<String> otherLines = new ArrayList<>();
            final CommandSourceStack source = user.getBase().createCommandSourceStack();
            for (final CommandNode<CommandSourceStack> node : ess.getServer().getCommands().getDispatcher().getRoot().getChildren()) {
                final String commandName = node.getName();
                if (registry.isEssentialsCommand(commandName) || commandName.contains(":")) {
                    continue;
                }
                if (!match.equalsIgnoreCase("") && !commandName.toLowerCase(Locale.ENGLISH).contains(match)) {
                    continue;
                }
                if (!node.canUse(source)) {
                    continue;
                }
                otherLines.add(Text.get().miniToLegacy(user.playerTl("helpLine", commandName, "")));
            }
            if (!otherLines.isEmpty()) {
                if (match.equalsIgnoreCase("")) {
                    lines.add(Text.get().miniToLegacy(user.playerTl("helpPlugin", "Minecraft", "minecraft")));
                }
                newLines.addAll(otherLines);
            }
        }
        lines.addAll(newLines);
    }

    @Override
    public List<String> getLines() {
        return lines;
    }

    @Override
    public List<String> getChapters() {
        return chapters;
    }

    @Override
    public Map<String, Integer> getBookmarks() {
        return bookmarks;
    }
}
