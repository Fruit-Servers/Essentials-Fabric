package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandgamemode extends EssentialsLoopCommand {
    private static final List<String> STANDARD_OPTIONS = List.of("creative", "survival", "adventure", "spectator", "toggle");

    public Commandgamemode() {
        super("gamemode");
    }

    /** Bukkit-style lowercase name (matches the translation keys {@code survival}, {@code creative}...). */
    public static String modeName(final GameType type) {
        return type.getName().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        } else if (args.length == 1) {
            loopOnlinePlayersConsumer(server, sender, false, true, args[0], user -> setUserGamemode(sender, matchGameMode(commandLabel), user));
        } else if (args.length == 2) {
            loopOnlinePlayersConsumer(server, sender, false, true, args[1], user -> setUserGamemode(sender, matchGameMode(args[0]), user));
        }
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        GameType gameMode;
        if (args.length == 0) {
            gameMode = matchGameMode(commandLabel);
        } else if (args.length > 1 && args[1].trim().length() > 2 && user.isAuthorized("essentials.gamemode.others")) {
            loopOnlinePlayersConsumer(server, user.getSource(), false, true, args[1], player -> setUserGamemode(user.getSource(), matchGameMode(args[0].toLowerCase(Locale.ENGLISH)), player));
            return;
        } else {
            try {
                gameMode = matchGameMode(args[0].toLowerCase(Locale.ENGLISH));
            } catch (final NotEnoughArgumentsException e) {
                if (user.isAuthorized("essentials.gamemode.others")) {
                    loopOnlinePlayersConsumer(server, user.getSource(), false, true, args[0], player -> setUserGamemode(user.getSource(), matchGameMode(commandLabel), player));
                    return;
                }
                throw new NotEnoughArgumentsException();
            }
        }
        final GameType current = user.getBase().gameMode.getGameModeForPlayer();
        if (gameMode == null) {
            gameMode = current == GameType.SURVIVAL ? GameType.CREATIVE : current == GameType.CREATIVE ? GameType.ADVENTURE : GameType.SURVIVAL;
        }
        if (isProhibitedChange(user, gameMode)) {
            user.sendTl("cantGamemode", user.playerTl(modeName(gameMode)));
            return;
        }
        user.getBase().setGameMode(gameMode);
        user.sendTl("gameMode", user.playerTl(modeName(user.getBase().gameMode.getGameModeForPlayer())), user.getDisplayName());
    }

    private void setUserGamemode(final CommandSource sender, final GameType gameMode, final User user) throws NotEnoughArgumentsException {
        if (gameMode == null) {
            throw new NotEnoughArgumentsException(sender.tl("gameModeInvalid"));
        }
        if (sender.isPlayer() && isProhibitedChange(sender.getUser(), gameMode)) {
            sender.sendTl("cantGamemode", gameMode.getName());
            return;
        }
        user.getBase().setGameMode(gameMode);
        sender.sendTl("gameMode", sender.tl(modeName(gameMode)), user.getDisplayName());
    }

    // essentials.gamemode will let them change to any but essentials.gamemode.survival would only let them change to survival.
    private boolean isProhibitedChange(final IUser user, final GameType to) {
        return user != null && !user.isAuthorized("essentials.gamemode.all") && !user.isAuthorized("essentials.gamemode." + modeName(to));
    }

    private GameType matchGameMode(String modeString) throws NotEnoughArgumentsException {
        GameType mode = null;
        modeString = modeString.toLowerCase(Locale.ENGLISH);
        if (modeString.equalsIgnoreCase("gmc") || modeString.equalsIgnoreCase("egmc") || modeString.contains("creat") || modeString.equalsIgnoreCase("1") || modeString.equalsIgnoreCase("c")) {
            mode = GameType.CREATIVE;
        } else if (modeString.equalsIgnoreCase("gms") || modeString.equalsIgnoreCase("egms") || modeString.contains("survi") || modeString.equalsIgnoreCase("0") || modeString.equalsIgnoreCase("s")) {
            mode = GameType.SURVIVAL;
        } else if (modeString.equalsIgnoreCase("gma") || modeString.equalsIgnoreCase("egma") || modeString.contains("advent") || modeString.equalsIgnoreCase("2") || modeString.equalsIgnoreCase("a")) {
            mode = GameType.ADVENTURE;
        } else if (modeString.equalsIgnoreCase("gmsp") || modeString.equalsIgnoreCase("egmsp") || modeString.contains("spec") || modeString.equalsIgnoreCase("3") || modeString.equalsIgnoreCase("sp")) {
            mode = GameType.SPECTATOR;
        } else if (!modeString.equalsIgnoreCase("gmt") && !modeString.equalsIgnoreCase("egmt") && !modeString.contains("toggle") && !modeString.contains("cycle") && !modeString.equalsIgnoreCase("t")) {
            throw new NotEnoughArgumentsException();
        }
        return mode;
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            try {
                matchGameMode(commandLabel);
                return getPlayers(sender);
            } catch (final NotEnoughArgumentsException e) {
                return STANDARD_OPTIONS;
            }
        } else if (args.length == 2) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        boolean isDirectGamemodeCommand;
        try {
            matchGameMode(commandLabel);
            isDirectGamemodeCommand = true;
        } catch (final NotEnoughArgumentsException ex) {
            isDirectGamemodeCommand = false;
        }
        if (args.length == 1) {
            if (user.isAuthorized("essentials.gamemode.others") && isDirectGamemodeCommand) {
                return getPlayers(user);
            } else {
                return STANDARD_OPTIONS;
            }
        } else if (args.length == 2 && user.isAuthorized("essentials.gamemode.others") && !isDirectGamemodeCommand) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
    }
}
