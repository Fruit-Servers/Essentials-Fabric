package net.essentialsx.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Registers every Essentials command and alias as a Brigadier literal with a greedy
 * argument, and runs the shared command pipeline (Section 7.3):
 * source validation → permission → jail/mute policy → cooldown → cost → execution → audit.
 */
public class CommandRegistry {
    /** Name of the greedy argument node that carries everything after the command label. */
    private static final String ARGS = "args";

    private final Essentials ess;
    private final Map<String, EssentialsCommand> commands = new LinkedHashMap<>();
    private final Map<String, CommandInfo> infos = new LinkedHashMap<>();
    private final Map<String, String> aliasToCommand = new HashMap<>();
    private final Set<String> registeredLiterals = new java.util.HashSet<>();
    /** Root literals present before any mod registered commands, i.e. vanilla's. */
    private final Set<String> vanillaLiterals = new java.util.HashSet<>();

    public CommandRegistry(final Essentials ess) {
        this.ess = ess;
    }

    public record CommandInfo(String name, List<String> aliases, String description, String usage, boolean spawnModule) {
    }

    public void register(final EssentialsCommand command, final List<String> aliases) {
        register(command, aliases, false);
    }

    public void register(final EssentialsCommand command, final List<String> aliases, final boolean spawnModule) {
        register(command, aliases, spawnModule, null, null);
    }

    /**
     * Registers a command; description/usage default to the {@code <name>CommandDescription} / {@code <name>CommandUsage}
     * translation keys when present (core commands) and otherwise to the supplied fallbacks (module commands).
     */
    public void register(final EssentialsCommand command, final List<String> aliases, final boolean spawnModule, final String fallbackDescription, final String fallbackUsage) {
        command.setEssentials(ess);
        commands.put(command.getName(), command);
        final String descriptionKey = command.getName() + "CommandDescription";
        final String usageKey = command.getName() + "CommandUsage";
        final String description = net.essentialsx.fabric.text.I18n.hasKey(descriptionKey) ? Text.get().miniToLegacy(tlLiteral(descriptionKey)) : (fallbackDescription == null ? "" : fallbackDescription);
        final String usage = net.essentialsx.fabric.text.I18n.hasKey(usageKey) ? Text.get().miniToLegacy(tlLiteral(usageKey)) : (fallbackUsage == null ? "/<command>" : fallbackUsage);
        infos.put(command.getName(), new CommandInfo(command.getName(), aliases, description, usage, spawnModule));
        aliasToCommand.put(command.getName(), command.getName());
        for (final String alias : aliases) {
            aliasToCommand.put(alias.toLowerCase(Locale.ENGLISH), command.getName());
        }
    }

    public EssentialsCommand getCommand(final String name) {
        return commands.get(name);
    }

    public CommandInfo getInfo(final String name) {
        return infos.get(name);
    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    public Map<String, CommandInfo> getCommandInfos() {
        return infos;
    }

    public boolean isEssentialsCommand(final String label) {
        return aliasToCommand.containsKey(label.toLowerCase(Locale.ENGLISH));
    }

    /** Resolve any alias/label to the primary command name, or null. */
    public String resolve(final String label) {
        return aliasToCommand.get(label.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Build Brigadier nodes for each command and alias.
     */
    public void registerAll(final CommandDispatcher<CommandSourceStack> dispatcher) {
        registeredLiterals.clear();
        for (final CommandInfo info : infos.values()) {
            final EssentialsCommand command = commands.get(info.name());
            if (ess.getSettings().isCommandDisabled(info.name())) {
                continue;
            }
            registerLiteral(dispatcher, renamed(info.name()), command, info.name());
            // namespaced form always available for collision resolution
            registerLiteral(dispatcher, "essentials:" + info.name(), command, info.name());
            for (final String alias : info.aliases()) {
                if (ess.getSettings().isCommandDisabled(alias)) {
                    continue;
                }
                registerLiteral(dispatcher, renamed(alias), command, alias);
            }
        }
    }

    /**
     * Applies the {@code command-name-overrides} config map, which moves an Essentials command or alias
     * onto a different literal instead of contesting the original name with another mod. The original
     * label is still what {@code disabled-commands}, {@code mute-commands} and the cooldowns match on.
     */
    private String renamed(final String label) {
        final String override = ess.getSettings().getCommandNameOverride(label);
        return override == null ? label : override;
    }

    /**
     * Records the literals vanilla registered. Runs in an event phase before every other mod's
     * {@code CommandRegistrationCallback}, so the snapshot does not contain mod commands.
     */
    public void snapshotVanilla(final CommandDispatcher<CommandSourceStack> dispatcher) {
        vanillaLiterals.clear();
        for (final CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
            vanillaLiterals.add(child.getName().toLowerCase(Locale.ENGLISH));
        }
    }

    public boolean isVanillaLiteral(final String literal) {
        return vanillaLiterals.contains(literal.toLowerCase(Locale.ENGLISH));
    }

    private void registerLiteral(final CommandDispatcher<CommandSourceStack> dispatcher, final String literal, final EssentialsCommand command, final String label) {
        final String lower = literal.toLowerCase(Locale.ENGLISH);
        if (registeredLiterals.contains(lower)) {
            return;
        }
        final CommandNode<CommandSourceStack> existing = dispatcher.getRoot().getChild(lower);
        if (existing != null && !lower.contains(":")) {
            final boolean vanilla = isVanillaLiteral(lower);
            if (vanilla) {
                // Bukkit parity: plugin commands beat vanilla ones; vanilla stays reachable as /minecraft:<name>.
                if (!ess.getSettings().isOverrideVanillaCommands() || ess.getSettings().isVanillaCommandKept(lower) || ess.getSettings().isVanillaCommandKept(command.getName())) {
                    ess.getLogger().info("Leaving vanilla /{} untouched (keep-vanilla-commands); Essentials' version is /essentials:{} or /e{}.", lower, command.getName(), command.getName());
                    return;
                }
                CommandMerger.removeRootLiteral(dispatcher, lower);
                if (dispatcher.getRoot().getChild("minecraft:" + lower) == null) {
                    dispatcher.getRoot().addChild(cloneLiteral(existing, "minecraft:" + lower));
                }
                if (ess.getSettings().isDebug()) {
                    ess.getLogger().info("Took over vanilla /{} (vanilla remains available as /minecraft:{}).", lower, lower);
                }
            } else if (ess.getSettings().isCommandOverridden(command.getName())) {
                // Admin asked for Essentials to win the label outright; the other mod loses it.
                CommandMerger.removeRootLiteral(dispatcher, lower);
            } else if (ess.getSettings().isMergeConflictingCommands() && existing.getRedirect() == null) {
                // Default: keep both. See mergeWithForeign.
                mergeWithForeign(dispatcher, existing, lower, command, label);
                return;
            } else if (!lower.equals("e" + command.getName())) {
                // Merging is off (or the node is a redirect, whose own children Brigadier never reads).
                // The other mod keeps the label; Essentials stays reachable under its own names.
                ess.getLogger().info("Command /{} is already registered by another mod; use /essentials:{} or /e{} (or add '{}' to overridden-commands).", lower, command.getName(), command.getName(), command.getName());
                return;
            } else {
                // The "e"-prefixed alias exists purely as Essentials' collision fallback, so it stays ours.
                CommandMerger.removeRootLiteral(dispatcher, lower);
            }
        }
        registeredLiterals.add(lower);
        final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(lower)
            .requires(source -> canUse(source, command))
            .executes(ctx -> execute(ctx.getSource(), command, label, lower, new String[0]))
            .then(argumentNode(ARGS, command, label, lower));
        final LiteralCommandNode<CommandSourceStack> registered = dispatcher.register(node);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("Registered /{} -> {}", registered.getName(), command.getName());
        }
    }

    /**
     * Keeps another mod's command and Essentials' version of it on the same literal instead of one
     * deleting the other, so {@code /eco shop list} reaches the other mod while {@code /eco give Notch 100}
     * reaches Essentials. {@link CommandMerger#merge} documents the routing and permission rules.
     */
    private void mergeWithForeign(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandNode<CommandSourceStack> existing, final String lower, final EssentialsCommand command, final String label) {
        final String argName = CommandMerger.freeArgumentName(existing, ARGS);
        final LiteralCommandNode<CommandSourceStack> merged = CommandMerger.merge(existing, lower,
            source -> canUse(source, command),
            ctx -> execute(ctx.getSource(), command, label, lower, new String[0]),
            argumentNode(argName, command, label, lower));
        CommandMerger.removeRootLiteral(dispatcher, lower);
        dispatcher.getRoot().addChild(merged);
        registeredLiterals.add(lower);
        ess.getLogger().info("Command /{} is also registered by another mod; merged both trees. Its subcommands still work and Essentials handles everything else; Essentials-only form is /essentials:{}.", lower, command.getName());
    }

    /** The greedy "everything after the label" argument that feeds the Essentials command pipeline. */
    private RequiredArgumentBuilder<CommandSourceStack, String> argumentNode(final String name, final EssentialsCommand command, final String label, final String display) {
        final SuggestionProvider<CommandSourceStack> suggestions = (ctx, builder) -> suggest(ctx, builder, command, label);
        return Commands.argument(name, StringArgumentType.greedyString())
            .requires(source -> canUse(source, command))
            .suggests(suggestions)
            .executes(ctx -> execute(ctx.getSource(), command, label, display, tokenize(StringArgumentType.getString(ctx, name))));
    }

    /** Copy of a root literal under a new name that shares the original's children, requirement and executor. */
    private static LiteralCommandNode<CommandSourceStack> cloneLiteral(final CommandNode<CommandSourceStack> original, final String name) {
        final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name).requires(original.getRequirement());
        if (original.getCommand() != null) {
            builder.executes(original.getCommand());
        }
        if (original.getRedirect() != null) {
            builder.forward(original.getRedirect(), original.getRedirectModifier(), original.isFork());
        }
        for (final CommandNode<CommandSourceStack> child : original.getChildren()) {
            builder.then(child);
        }
        return builder.build();
    }

    private static String[] tokenize(final String args) {
        final String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split(" +");
    }

    private boolean canUse(final CommandSourceStack source, final EssentialsCommand command) {
        final ServerPlayer player = source.getPlayer();
        if (player == null) {
            return true;
        }
        final User user = ess.getUser(player);
        if (user == null) {
            return false;
        }
        if (!user.isAuthorized(command)) {
            return false;
        }
        return !user.isJailed() || user.isAuthorized(command, "essentials.jail.allow.");
    }

    private CompletableFuture<Suggestions> suggest(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder, final EssentialsCommand command, final String label) {
        final String remaining = builder.getRemaining();
        final String[] args = remaining.isEmpty() ? new String[] {""} : remaining.endsWith(" ") ? append(remaining.trim().split(" +"), "") : remaining.split(" +");
        final CommandSource sender = new CommandSource(ess, ctx.getSource());
        List<String> options;
        try {
            if (sender.isPlayer()) {
                final User user = ess.getUser(sender.getPlayer());
                if (!user.isAuthorized(command)) {
                    return builder.buildFuture();
                }
                options = command.tabComplete(ess.getServer(), user, label, args);
            } else {
                options = command.tabComplete(ess.getServer(), sender, label, args);
            }
        } catch (final Exception ex) {
            options = Collections.emptyList();
        }
        if (options == null) {
            options = Collections.emptyList();
        }
        final int lastSpace = remaining.lastIndexOf(' ');
        final SuggestionsBuilder offset = builder.createOffset(builder.getStart() + lastSpace + 1);
        final int limit = 200;
        int count = 0;
        for (final String option : options) {
            if (option == null || option.isEmpty()) {
                continue;
            }
            offset.suggest(option);
            if (++count >= limit) {
                break;
            }
        }
        return offset.buildFuture();
    }

    private static String[] append(final String[] array, final String value) {
        final String[] result = new String[array.length + 1];
        System.arraycopy(array, 0, result, 0, array.length);
        result[array.length] = value;
        return result;
    }

    /**
     * Shared execution pipeline (upstream {@code onCommandEssentials}).
     */
    public int execute(final CommandSourceStack source, final EssentialsCommand cmd, final String commandLabel, final String[] args) {
        return execute(source, cmd, commandLabel, commandLabel, args);
    }

    /**
     * Shared execution pipeline (upstream {@code onCommandEssentials}).
     *
     * <p>{@code displayLabel} is the literal the sender actually typed, which differs from
     * {@code commandLabel} when {@code command-name-overrides} moved the command onto another name.
     * Config policy (disabled commands, mute/social spy lists, cooldowns) matches on {@code commandLabel}
     * so it keeps using the documented Essentials names; everything the sender reads uses
     * {@code displayLabel} so usage lines quote a command that actually exists.
     */
    public int execute(final CommandSourceStack source, final EssentialsCommand cmd, final String commandLabel, final String displayLabel, final String[] args) {
        try {
            User user = null;
            final ServerPlayer player = source.getPlayer();
            if (player != null) {
                user = ess.getUser(player);
            } else if (source.getEntity() == null && !source.getTextName().equals("Server") && ess.getSettings().logCommandBlockCommands() && source.getPosition() != null && isCommandBlock(source)) {
                ess.getLogger().info("CommandBlock at " + source.getPosition().x + "," + source.getPosition().y + "," + source.getPosition().z + " issued server command: /" + displayLabel + " " + EssentialsCommand.getFinalArg(args, 0));
            } else if (ess.getSettings().logConsoleCommands() && !isCommandBlock(source)) {
                ess.getLogger().info(source.getTextName() + " issued server command: /" + displayLabel + " " + EssentialsCommand.getFinalArg(args, 0));
            }
            final CommandSource sender = new CommandSource(ess, source);
            if (user != null && !ess.getSettings().isCommandDisabled("mail") && !cmd.getName().equals("mail") && user.isAuthorized("essentials.mail")) {
                user.notifyOfMail();
            }
            if (commandLabel.equalsIgnoreCase("essversion")) {
                sender.sendMessage("This server is running Essentials Fabric " + Essentials.VERSION);
                return 1;
            }
            if (ess.getSettings().isCommandDisabled(commandLabel)) {
                sender.sendTl("commandDisabled", displayLabel);
                return 0;
            }
            if (user != null && !user.isAuthorized(cmd)) {
                ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("deniedAccessCommand", user.getName())));
                user.sendTl("noAccessCommand");
                return 0;
            }
            if (user != null && user.isJailed() && !user.isAuthorized(cmd, "essentials.jail.allow.")) {
                if (user.getJailTimeout() > 0) {
                    user.sendTl("playerJailedFor", user.getName(), user.getFormattedJailTime());
                } else {
                    user.sendTl("jailMessage");
                }
                return 0;
            }
            if (user != null && !preprocess(user, cmd, commandLabel, args)) {
                return 0;
            }
            try {
                if (user == null) {
                    cmd.executeSource(ess.getServer(), sender, commandLabel, args);
                } else {
                    cmd.execute(ess.getServer(), user, commandLabel, args);
                }
                return 1;
            } catch (final NoChargeException | QuietAbortException ex) {
                return 1;
            } catch (final NotEnoughArgumentsException ex) {
                final CommandInfo info = infos.get(cmd.getName());
                if (ess.getSettings().isVerboseCommandUsages() && !cmd.getUsageStrings().isEmpty()) {
                    sender.sendTl("commandHelpLine1", displayLabel);
                    String description = info == null ? "" : info.description();
                    try {
                        description = net.essentialsx.fabric.text.I18n.hasKey(cmd.getName() + "CommandDescription") ? sender.tl(cmd.getName() + "CommandDescription") : getInfo(cmd.getName()).description();
                    } catch (final MissingResourceException ignored) {
                    }
                    sender.sendTl("commandHelpLine2", description);
                    sender.sendTl("commandHelpLine3");
                    for (final Map.Entry<String, String> usage : cmd.getUsageStrings().entrySet()) {
                        sender.sendTl("commandHelpLineUsage", Text.parsed(usage.getKey().replace("<command>", displayLabel)), Text.parsed(sender.tl(usage.getValue())));
                    }
                } else {
                    sender.sendMessage(info == null ? "" : info.description());
                    sender.sendMessage(info == null ? "" : info.usage().replace("<command>", displayLabel));
                }
                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    sender.sendComponent(Text.get().deserializeMiniMessage(ex.getMessage()));
                }
                if (ex.getCause() != null && ess.getSettings().isDebug()) {
                    ess.getLogger().error("Command argument error", ex.getCause());
                }
                return 0;
            } catch (final Exception ex) {
                ess.showError(sender, ex, displayLabel);
                if (ess.getSettings().isDebug()) {
                    ess.getLogger().error("Command error", ex);
                }
                return 0;
            }
        } catch (final Throwable ex) {
            ess.getLogger().error(Text.get().miniToLegacy(tlLiteral("commandFailed", displayLabel)), ex);
            return 0;
        }
    }

    private static boolean isCommandBlock(final CommandSourceStack source) {
        return source.getEntity() == null && !"Server".equals(source.getTextName()) && !"Rcon".equals(source.getTextName());
    }

    /**
     * Player command pre-processing that upstream performs in
     * {@code PlayerCommandPreprocessEvent}: social spy, mute-commands, AFK activity and
     * command cooldowns. Returns false when the command must be cancelled.
     */
    public boolean preprocess(final User user, final EssentialsCommand pluginCommand, final String label, final String[] args) {
        final String cmd = label.toLowerCase(Locale.ENGLISH);
        final String argString = args.length == 0 ? "" : " " + String.join(" ", args);
        final String message = "/" + cmd + argString;
        if (ess.getSettings().getSocialSpyCommands().contains(cmd) || ess.getSettings().getSocialSpyCommands().contains("*")) {
            if (pluginCommand == null || !pluginCommand.getName().equals("msg") && !pluginCommand.getName().equals("r")) {
                if (!user.isAuthorized("essentials.chat.spy.exempt")) {
                    final String playerName = ess.getSettings().isSocialSpyDisplayNames() ? user.getDisplayName() : user.getName();
                    for (final User spyer : ess.getOnlineUsers()) {
                        if (spyer.isSocialSpyEnabled() && !user.equals(spyer)) {
                            final net.kyori.adventure.text.Component base = (user.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers())
                                ? spyer.tlComponent("socialSpyMutedPrefix")
                                : spyer.tlComponent("socialSpyPrefix");
                            final net.kyori.adventure.text.Component formatted = Text.get().deserializeMiniMessage(spyer.playerTl("socialSpyCmdFormat", playerName, message));
                            spyer.sendComponent(base.append(formatted));
                        }
                    }
                }
            }
        }
        if (user.isMuted() && (ess.getSettings().getMuteCommands().contains(cmd) || ess.getSettings().getMuteCommands().contains("*"))) {
            user.notifyMuted();
            ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("mutedUserSpeaks", user.getName(), message)));
            return false;
        }
        boolean broadcast = true;
        boolean update = true;
        if (pluginCommand != null) {
            switch (pluginCommand.getName()) {
                case "afk":
                    update = false;
                case "vanish":
                    broadcast = false;
                    break;
                default:
                    break;
            }
        }
        if (update) {
            user.updateActivityOnInteract(broadcast);
        }
        if (ess.getSettings().isCommandCooldownsEnabled()
            && !user.isAuthorized("essentials.commandcooldowns.bypass")
            && (pluginCommand == null || !user.isAuthorized("essentials.commandcooldowns.bypass." + pluginCommand.getName()))) {
            final String fullCommand = pluginCommand == null ? cmd + argString : pluginCommand.getName() + argString;
            boolean cooldownFound = false;
            for (final Map.Entry<Pattern, Long> entry : user.getCommandCooldowns().entrySet()) {
                if (entry.getValue() <= System.currentTimeMillis()) {
                    user.clearCommandCooldown(entry.getKey());
                } else if (entry.getKey().matcher(fullCommand).matches()) {
                    final String commandCooldownTime = DateUtil.formatDateDiff(entry.getValue());
                    user.sendTl("commandCooldown", commandCooldownTime);
                    cooldownFound = true;
                    return false;
                }
            }
            if (!cooldownFound) {
                final Map.Entry<Pattern, Long> cooldownEntry = ess.getSettings().getCommandCooldownEntry(fullCommand);
                if (cooldownEntry != null) {
                    if (ess.getSettings().isDebug()) {
                        ess.getLogger().info("Applying " + cooldownEntry.getValue() + "ms cooldown on /" + fullCommand + " for" + user.getName() + ".");
                    }
                    final Date expiry = new Date(System.currentTimeMillis() + cooldownEntry.getValue());
                    user.addCommandCooldown(cooldownEntry.getKey(), expiry, ess.getSettings().isCommandCooldownPersistent(fullCommand));
                }
            }
        }
        return true;
    }

    /**
     * Dispatch a command string on behalf of a user through the normal pipeline
     * (powertools, signs, sudo).
     */
    public void dispatchAsUser(final User user, final String command) {
        if (user.getBase() == null) {
            return;
        }
        final String cmd = command.startsWith("/") ? command.substring(1) : command;
        ess.getServer().getCommands().performPrefixedCommand(user.getBase().createCommandSourceStack(), cmd);
    }

    public List<String> getAllLabels() {
        return new ArrayList<>(aliasToCommand.keySet());
    }
}
