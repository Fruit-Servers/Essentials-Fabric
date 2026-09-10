package net.essentialsx.fabric.command;

import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class EssentialsToggleCommand extends EssentialsCommand {
    final String othersPermission;

    public EssentialsToggleCommand(final String command, final String othersPermission) {
        super(command);
        this.othersPermission = othersPermission;
    }

    /**
     * Whether this player may toggle the feature for other players. Defaults to the usual
     * permission resolution (provider, wildcards, then op / player-command fallbacks).
     */
    protected boolean canToggleOthers(final User user) {
        return user.isAuthorized(othersPermission);
    }

    protected void handleToggleWithArgs(final MinecraftServer server, final User user, final String[] args) throws Exception {
        if (args.length == 1) {
            final Boolean toggle = matchToggleArgument(args[0]);
            if (toggle == null && canToggleOthers(user)) {
                toggleOtherPlayers(server, user.getSource(), args);
            } else {
                togglePlayer(user.getSource(), user, toggle);
            }
        } else if (args.length == 2 && canToggleOthers(user)) {
            toggleOtherPlayers(server, user.getSource(), args);
        } else {
            togglePlayer(user.getSource(), user, null);
        }
    }

    protected Boolean matchToggleArgument(final String arg) {
        if (arg.equalsIgnoreCase("on") || arg.startsWith("ena") || arg.equalsIgnoreCase("1")) {
            return true;
        } else if (arg.equalsIgnoreCase("off") || arg.startsWith("dis") || arg.equalsIgnoreCase("0")) {
            return false;
        }
        return null;
    }

    protected void toggleOtherPlayers(final MinecraftServer server, final CommandSource sender, final String[] args) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (args.length < 1 || args[0].trim().length() < 2) {
            throw new PlayerNotFoundException();
        }
        final boolean skipHidden = sender.isPlayer() && !ess.getUser(sender.getPlayer()).canInteractVanished();
        boolean foundUser = false;
        final List<ServerPlayer> matchedPlayers = ess.matchPlayers(args[0]);
        for (final ServerPlayer matchPlayer : matchedPlayers) {
            final User player = ess.getUser(matchPlayer);
            if (skipHidden && player.isHidden(sender.getPlayer()) && player.isHiddenFrom(sender.getPlayer())) {
                continue;
            }
            foundUser = true;
            if (args.length > 1) {
                final Boolean toggle = matchToggleArgument(args[1]);
                togglePlayer(sender, player, toggle);
            } else {
                togglePlayer(sender, player, null);
            }
        }
        if (!foundUser) {
            throw new PlayerNotFoundException();
        }
    }

    protected abstract void togglePlayer(CommandSource sender, User user, Boolean enabled) throws NotEnoughArgumentsException;

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            if (canToggleOthers(user)) {
                return getPlayers(user);
            } else {
                return new ArrayList<>(List.of("enable", "disable"));
            }
        } else if (args.length == 2 && canToggleOthers(user)) {
            return new ArrayList<>(List.of("enable", "disable"));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("enable", "disable"));
        } else {
            return Collections.emptyList();
        }
    }
}
