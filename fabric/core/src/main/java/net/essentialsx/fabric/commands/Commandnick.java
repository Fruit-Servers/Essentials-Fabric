package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class Commandnick extends EssentialsLoopCommand {
    public Commandnick() {
        super("nick");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length > 1 && user.isAuthorized("essentials.nick.others")) {
            loopOfflinePlayers(server, user.getSource(), false, true, args[0], formatNickname(user, args[1]).split(" "));
            user.sendTl("nickChanged");
        } else {
            updatePlayer(server, user.getSource(), user, formatNickname(user, args[0]).split(" "));
        }
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        loopOfflinePlayers(server, sender, false, true, args[0], formatNickname(null, args[1]).split(" "));
        sender.sendTl("nickChanged");
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User target, final String[] args) throws NotEnoughArgumentsException {
        final String nick = args[0];
        if ("off".equalsIgnoreCase(nick)) {
            setNickname(target, null);
            target.sendTl("nickNoMore");
        } else if (target.getName().equalsIgnoreCase(nick)) {
            setNickname(target, nick);
            final String strippedDisplay = FormatUtil.stripFormat(target.getDisplayName());
            if (strippedDisplay != null && !strippedDisplay.equalsIgnoreCase(target.getName())) {
                target.sendTl("nickNoMore");
            }
            target.sendTl("nickSet", ess.getSettings().changeDisplayName() ? target.getDisplayName() : nick);
        } else if (nickInUse(target, nick)) {
            throw new NotEnoughArgumentsException(sender.tl("nickInUse"));
        } else {
            setNickname(target, nick);
            target.sendTl("nickSet", ess.getSettings().changeDisplayName() ? target.getDisplayName() : nick);
        }
    }

    private String formatNickname(final User user, final String nick) throws Exception {
        final String newNick = user == null ? FormatUtil.replaceFormat(nick) : FormatUtil.formatString(user, "essentials.nick", nick);
        if (!newNick.matches(ess.getSettings().getNickRegex()) && user != null && !user.isAuthorized("essentials.nick.allowunsafe")) {
            throw new TranslatableException("nickNamesAlpha");
        } else if (getNickLength(newNick) > ess.getSettings().getMaxNickLength()) {
            throw new TranslatableException("nickTooLong");
        } else if (FormatUtil.stripFormat(newNick).length() < 1) {
            throw new TranslatableException("nickNamesAlpha");
        } else if (user != null && user.isAuthorized("essentials.nick.changecolors") && !user.isAuthorized("essentials.nick.changecolors.bypass") && !FormatUtil.stripFormat(newNick).equals(user.getName()) && !nick.equalsIgnoreCase("off")) {
            throw new TranslatableException("nickNamesOnlyColorChanges");
        } else if (user != null && !user.isAuthorized("essentials.nick.blacklist.bypass") && isNickBanned(newNick)) {
            throw new TranslatableException("nickNameBlacklist", nick);
        }
        return newNick;
    }

    private boolean isNickBanned(final String newNick) {
        for (final Predicate<String> predicate : ess.getSettings().getNickBlacklist()) {
            if (predicate.test(newNick)) {
                return true;
            }
        }
        return false;
    }

    private int getNickLength(final String nick) {
        if (ess.getSettings().ignoreColorsInMaxLength()) {
            return FormatUtil.stripFormat(nick).length();
        }
        return FormatUtil.unformatString(nick).length();
    }

    private boolean nickInUse(final User target, final String nick) {
        final String lowerNick = FormatUtil.stripFormat(nick.toLowerCase(Locale.ENGLISH));
        for (final User onlinePlayer : ess.getOnlineUsers()) {
            if (target.getName().equals(onlinePlayer.getName())) {
                continue;
            }
            final String matchNick = FormatUtil.stripFormat(onlinePlayer.getNickname());
            if ((matchNick != null && !matchNick.isEmpty() && lowerNick.equals(matchNick.toLowerCase(Locale.ENGLISH))) || lowerNick.equals(onlinePlayer.getName().toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        final User fetchedUser = ess.getUser(lowerNick);
        return fetchedUser != null && fetchedUser != target;
    }

    private void setNickname(final User target, final String nickname) {
        target.setNickname(nickname);
        target.setDisplayNick();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.nick.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
