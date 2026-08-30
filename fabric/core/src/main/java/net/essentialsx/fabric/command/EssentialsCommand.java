package net.essentialsx.fabric.command;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Base class for every Essentials command (mirrors upstream {@code EssentialsCommand}).
 * Commands receive already-tokenised arguments; Brigadier registration is handled by
 * {@link CommandRegistry}.
 */
public abstract class EssentialsCommand {
    public static final List<String> COMMON_DURATIONS = List.of("1", "60", "600", "3600", "86400");
    protected static final List<String> COMMON_DATE_DIFFS = List.of("1m", "15m", "1h", "3h", "12h", "1d", "1w", "1mo", "1y");
    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("([ :>])(([\\[<])[A-Za-z |]+[>\\]])");
    private final transient String name;
    private final transient Map<String, String> usageStrings = new LinkedHashMap<>();
    protected transient Essentials ess;

    protected EssentialsCommand(final String name) {
        this.name = name;
        int i = 1;
        try {
            while (true) {
                final String baseKey = name + "CommandUsage" + i;
                if (!I18n.hasKey(baseKey)) {
                    break;
                }
                addUsageString(tlLiteral(baseKey), baseKey + "Description");
                i++;
            }
        } catch (final MissingResourceException ignored) {
        }
    }

    private void addUsageString(final String usage, final String description) {
        final StringBuilder buffer = new StringBuilder();
        final Matcher matcher = ARGUMENT_PATTERN.matcher(usage);
        while (matcher.find()) {
            final String color = matcher.group(3).equals("<") ? tlLiteral("commandArgumentRequired") : tlLiteral("commandArgumentOptional");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + color + matcher.group(2).replace("|", tlLiteral("commandArgumentOr") + "|" + color) + "<reset>"));
        }
        matcher.appendTail(buffer);
        usageStrings.put(buffer.toString(), description);
    }

    public Map<String, String> getUsageStrings() {
        return usageStrings;
    }

    public static String getFinalArg(final String[] args, final int start) {
        final StringBuilder bldr = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i != start) {
                bldr.append(" ");
            }
            bldr.append(args[i]);
        }
        return bldr.toString();
    }

    public void setEssentials(final Essentials ess) {
        this.ess = ess;
    }

    public String getName() {
        return name;
    }

    // ---------------------------------------------------------------- player resolution

    protected User getPlayer(final MinecraftServer server, final CommandSource sender, final String[] args, final int pos) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, sender, args, pos, false);
    }

    protected User getPlayer(final MinecraftServer server, final CommandSource sender, final String[] args, final int pos, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (sender.isPlayer()) {
            final User user = ess.getUser(sender.getPlayer());
            return getPlayer(server, user, args, pos, getOffline);
        }
        return getPlayer(server, args, pos, true, getOffline);
    }

    protected User getPlayer(final MinecraftServer server, final CommandSource sender, final String searchTerm) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (sender.isPlayer()) {
            final User user = ess.getUser(sender.getPlayer());
            return getPlayer(server, user, searchTerm, user.canInteractVanished(), false);
        }
        return getPlayer(server, searchTerm, true, false);
    }

    protected User getPlayer(final MinecraftServer server, final User user, final String[] args, final int pos) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, user, args, pos, false);
    }

    protected User getPlayer(final MinecraftServer server, final User user, final String[] args, final int pos, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, user, args, pos, user.canInteractVanished(), getOffline);
    }

    protected User getPlayer(final MinecraftServer server, final String[] args, final int pos, final boolean getHidden, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, null, args, pos, getHidden, getOffline);
    }

    User getPlayer(final MinecraftServer server, final User sourceUser, final String[] args, final int pos, final boolean getHidden, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (args.length <= pos) {
            throw new NotEnoughArgumentsException();
        }
        if (args[pos].isEmpty()) {
            throw new PlayerNotFoundException();
        }
        return getPlayer(server, sourceUser, args[pos], getHidden, getOffline);
    }

    protected User getPlayer(final MinecraftServer server, final String searchTerm, final boolean getHidden, final boolean getOffline) throws PlayerNotFoundException {
        return getPlayer(server, null, searchTerm, getHidden, getOffline);
    }

    private User getPlayer(final MinecraftServer server, final User sourceUser, final String searchTerm, final boolean getHidden, final boolean getOffline) throws PlayerNotFoundException {
        return ess.matchUser(sourceUser, searchTerm, getHidden, getOffline);
    }

    // ---------------------------------------------------------------- execution

    /** Pipeline entry for player execution: cost pre-check, command body, cost charge. */
    public final void execute(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        run(server, user, commandLabel, args);
        charge.charge(user);
    }

    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        run(server, user.getSource(), commandLabel, args);
    }

    /** Pipeline entry for console/command-block execution. */
    public final void executeSource(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        run(server, sender, commandLabel, args);
    }

    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        throw new TranslatableException("onlyPlayers", commandLabel);
    }

    // ---------------------------------------------------------------- tab completion

    public final List<String> tabComplete(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        final List<String> options = getTabCompleteOptions(server, user, commandLabel, args);
        if (options == null) {
            return null;
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<>());
    }

    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        return getTabCompleteOptions(server, user.getSource(), commandLabel, args);
    }

    public final List<String> tabComplete(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        final List<String> options = getTabCompleteOptions(server, sender, commandLabel, args);
        if (options == null) {
            return null;
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<>());
    }

    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        return getPlayers(sender);
    }

    boolean canInteractWith(final CommandSource interactor, final User interactee) {
        return ess.canInteractWith(interactor, interactee);
    }

    protected List<String> getPlayers(final CommandSource interactor) {
        final List<String> players = new ArrayList<>();
        for (final User user : ess.getOnlineUsers()) {
            if (canInteractWith(interactor, user)) {
                players.add(ess.getSettings().changeTabCompleteName() ? FormatUtil.stripFormat(user.getDisplayName()) : user.getName());
            }
        }
        return players;
    }

    protected List<String> getPlayers(final User interactor) {
        return getPlayers(interactor.getSource());
    }

    protected List<String> getItems() {
        return new ArrayList<>(ess.getItemDb().listNames());
    }

    protected List<String> getMatchingItems(final String arg) {
        final List<String> items = new ArrayList<>(List.of("hand", "inventory", "blocks"));
        if (!arg.isEmpty()) {
            items.addAll(getItems());
        }
        return items;
    }

    protected final List<String> getCommands(final MinecraftServer server) {
        final List<String> commands = new ArrayList<>();
        for (final var node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            if (!node.getName().contains(":")) {
                commands.add(node.getName());
            }
        }
        return commands;
    }

    protected final List<String> getMods() {
        final List<String> mods = new ArrayList<>();
        for (final net.fabricmc.loader.api.ModContainer mod : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods()) {
            mods.add(mod.getMetadata().getName());
        }
        return mods;
    }

    protected final List<String> getWorlds(final MinecraftServer server) {
        return net.essentialsx.fabric.utils.Worlds.names(server);
    }

    /**
     * Delegate tab completion for a nested command line (used by sudo/powertool).
     */
    protected final List<String> tabCompleteCommand(final CommandSource sender, final MinecraftServer server, final String label, final String[] args, final int index) {
        final int numArgs = args.length - index - 1;
        final String[] effectiveArgs = new String[Math.max(0, numArgs)];
        if (numArgs > 0) {
            System.arraycopy(args, index, effectiveArgs, 0, numArgs);
        }
        final String line = label + (effectiveArgs.length == 0 ? " " : " " + String.join(" ", effectiveArgs));
        try {
            final var dispatcher = server.getCommands().getDispatcher();
            final var parse = dispatcher.parse(line, sender.getSender());
            final var suggestions = dispatcher.getCompletionSuggestions(parse).get();
            final List<String> result = new ArrayList<>();
            for (final var s : suggestions.getList()) {
                result.add(s.getText());
            }
            return result;
        } catch (final Exception ex) {
            return Collections.emptyList();
        }
    }

    public void showError(final CommandSource sender, final Throwable throwable, final String commandLabel) {
        ess.showError(sender, throwable, commandLabel);
    }

    public CompletableFuture<Boolean> getNewExceptionFuture(final CommandSource sender, final String commandLabel) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.exceptionally(e -> {
            showError(sender, e, commandLabel);
            return false;
        });
        return future;
    }

    protected static String[] subArray(final String[] args, final int from) {
        if (from >= args.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(args, from, args.length);
    }
}
