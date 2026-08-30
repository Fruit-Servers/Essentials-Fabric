package net.essentialsx.fabric.command;

import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class EssentialsLoopCommand extends EssentialsCommand {
    public EssentialsLoopCommand(final String command) {
        super(command);
    }

    protected void loopOfflinePlayers(final MinecraftServer server, final CommandSource sender, final boolean multipleStringMatches, final boolean matchWildcards, final String searchTerm, final String[] commandArgs) throws TranslatableException, NotEnoughArgumentsException {
        loopOfflinePlayersConsumer(server, sender, multipleStringMatches, matchWildcards, searchTerm, user -> updatePlayer(server, sender, user, commandArgs));
    }

    protected void loopOfflinePlayersConsumer(final MinecraftServer server, final CommandSource sender, final boolean multipleStringMatches, final boolean matchWildcards, final String searchTerm, final UserConsumer userConsumer) throws TranslatableException, NotEnoughArgumentsException {
        if (searchTerm.isEmpty()) {
            throw new PlayerNotFoundException();
        }
        if (sender.isPlayer() && (searchTerm.equals("@s") || searchTerm.equals("@p"))) {
            userConsumer.accept(sender.getUser());
            return;
        }
        final UUID uuid = StringUtil.toUUID(searchTerm);
        if (uuid != null) {
            final User matchedUser = ess.getUser(uuid);
            if (matchedUser == null) {
                throw new PlayerNotFoundException();
            }
            userConsumer.accept(matchedUser);
        } else if (matchWildcards && searchTerm.contentEquals("**")) {
            for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                final User user = ess.getUser(u);
                if (user != null) {
                    userConsumer.accept(user);
                }
            }
        } else if (matchWildcards && searchTerm.contentEquals("*")) {
            final boolean skipHidden = sender.isPlayer() && !ess.getUser(sender.getPlayer()).canInteractVanished();
            for (final User onlineUser : ess.getOnlineUsers()) {
                if (skipHidden && onlineUser.isHidden(sender.getPlayer()) && onlineUser.isHiddenFrom(sender.getPlayer())) {
                    continue;
                }
                userConsumer.accept(onlineUser);
            }
        } else if (multipleStringMatches) {
            if (searchTerm.trim().length() < 3) {
                throw new PlayerNotFoundException();
            }
            final List<ServerPlayer> matchedPlayers = ess.matchPlayers(searchTerm);
            if (matchedPlayers.isEmpty()) {
                final User matchedUser = getPlayer(server, searchTerm, true, true);
                userConsumer.accept(matchedUser);
            }
            for (final ServerPlayer matchPlayer : matchedPlayers) {
                final User matchedUser = ess.getUser(matchPlayer);
                userConsumer.accept(matchedUser);
            }
        } else {
            final User user = getPlayer(server, searchTerm, true, true);
            userConsumer.accept(user);
        }
    }

    protected void loopOnlinePlayers(final MinecraftServer server, final CommandSource sender, final boolean multipleStringMatches, final boolean matchWildcards, final String searchTerm, final String[] commandArgs) throws TranslatableException, NotEnoughArgumentsException {
        loopOnlinePlayersConsumer(server, sender, multipleStringMatches, matchWildcards, searchTerm, user -> updatePlayer(server, sender, user, commandArgs));
    }

    protected void loopOnlinePlayersConsumer(final MinecraftServer server, final CommandSource sender, final boolean multipleStringMatches, final boolean matchWildcards, final String searchTerm, final UserConsumer userConsumer) throws NotEnoughArgumentsException, TranslatableException {
        if (searchTerm.isEmpty()) {
            throw new PlayerNotFoundException();
        }
        if (sender.isPlayer() && (searchTerm.equals("@s") || searchTerm.equals("@p"))) {
            userConsumer.accept(sender.getUser());
            return;
        }
        final boolean skipHidden = sender.isPlayer() && !ess.getUser(sender.getPlayer()).canInteractVanished();
        if (matchWildcards && (searchTerm.contentEquals("**") || searchTerm.contentEquals("*"))) {
            for (final User onlineUser : ess.getOnlineUsers()) {
                if (skipHidden && onlineUser.isHidden(sender.getPlayer()) && onlineUser.isHiddenFrom(sender.getPlayer())) {
                    continue;
                }
                userConsumer.accept(onlineUser);
            }
        } else if (multipleStringMatches) {
            if (searchTerm.trim().length() < 2) {
                throw new PlayerNotFoundException();
            }
            boolean foundUser = false;
            final List<ServerPlayer> matchedPlayers = ess.matchPlayers(searchTerm);
            if (matchedPlayers.isEmpty()) {
                final String matchText = searchTerm.toLowerCase(Locale.ENGLISH);
                for (final User player : ess.getOnlineUsers()) {
                    if (skipHidden && player.isHidden(sender.getPlayer()) && player.isHiddenFrom(sender.getPlayer())) {
                        continue;
                    }
                    final String displayName = FormatUtil.stripFormat(player.getDisplayName()).toLowerCase(Locale.ENGLISH);
                    if (displayName.contains(matchText)) {
                        foundUser = true;
                        userConsumer.accept(player);
                    }
                }
            } else {
                for (final ServerPlayer matchPlayer : matchedPlayers) {
                    final User player = ess.getUser(matchPlayer);
                    if (skipHidden && player.isHidden(sender.getPlayer()) && player.isHiddenFrom(sender.getPlayer())) {
                        continue;
                    }
                    foundUser = true;
                    userConsumer.accept(player);
                }
            }
            if (!foundUser) {
                throw new PlayerNotFoundException();
            }
        } else {
            final User player = getPlayer(server, sender, searchTerm);
            userConsumer.accept(player);
        }
    }

    protected abstract void updatePlayer(MinecraftServer server, CommandSource sender, User user, String[] args) throws NotEnoughArgumentsException, PlayerExemptException, ChargeException, MaxMoneyException;

    @Override
    protected List<String> getPlayers(final CommandSource interactor) {
        final List<String> players = super.getPlayers(interactor);
        players.add("**");
        players.add("*");
        return players;
    }

    @Override
    protected List<String> getPlayers(final User interactor) {
        final List<String> players = super.getPlayers(interactor);
        players.add("**");
        players.add("*");
        return players;
    }

    public interface UserConsumer {
        void accept(User user) throws NotEnoughArgumentsException, TranslatableException;
    }
}
