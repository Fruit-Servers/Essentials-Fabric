package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.SetExpFix;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandexp extends EssentialsLoopCommand {
    public Commandexp() {
        super("exp");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final IUser user = sender.getUser();
        if (args.length == 0 || (args.length < 2 && user == null)) {
            if (user == null) {
                throw new NotEnoughArgumentsException();
            }
            showExp(sender, user);
            return;
        }
        final ExpCommands cmd;
        try {
            cmd = ExpCommands.valueOf(args[0].toUpperCase(Locale.ENGLISH));
        } catch (final Exception ex) {
            throw new NotEnoughArgumentsException(ex);
        }
        if (!cmd.hasPermission(user)) {
            user.sendTl("noAccessSubCommand", "/" + commandLabel + " " + cmd.name().toLowerCase(Locale.ENGLISH));
            return;
        }
        switch (cmd) {
            case SET: {
                if (args.length == 3 && cmd.hasOtherPermission(user)) {
                    loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> setExp(sender, player, args[2], false));
                } else if (args.length == 2 && user != null) {
                    setExp(sender, user, args[1], false);
                } else {
                    throw new NotEnoughArgumentsException();
                }
                return;
            }
            case GIVE: {
                if (args.length == 3 && cmd.hasOtherPermission(user)) {
                    loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> setExp(sender, player, args[2], true));
                } else if (args.length == 2 && user != null) {
                    setExp(sender, user, args[1], true);
                } else {
                    throw new NotEnoughArgumentsException();
                }
                return;
            }
            case TAKE: {
                if (args.length == 3 && cmd.hasOtherPermission(user)) {
                    loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> setExp(sender, player, "-" + args[2], true));
                } else if (args.length == 2 && user != null) {
                    setExp(sender, user, "-" + args[1], true);
                } else {
                    throw new NotEnoughArgumentsException();
                }
                return;
            }
            case RESET: {
                if (args.length == 2 && cmd.hasOtherPermission(user)) {
                    loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> setExp(sender, player, "0", false));
                } else if (user != null) {
                    setExp(sender, user, "0", false);
                } else {
                    throw new NotEnoughArgumentsException();
                }
                return;
            }
            case SHOW: {
                if (args.length == 2 && (user == null || user.isAuthorized("essentials.exp.others"))) {
                    showExp(sender, getPlayer(server, sender, args[1]));
                } else if (user != null) {
                    showExp(sender, user);
                } else {
                    throw new NotEnoughArgumentsException();
                }
                return;
            }
        }
        throw new NotEnoughArgumentsException();
    }

    private void showExp(final CommandSource sender, final IUser target) {
        sender.sendTl("exp", target.getDisplayName(), SetExpFix.getTotalExperience(target.getBase()), target.getBase().experienceLevel, SetExpFix.getExpUntilNextLevel(target.getBase()));
    }

    private void setExp(final CommandSource sender, final IUser target, String strAmount, final boolean give) throws NotEnoughArgumentsException {
        long amount;
        strAmount = strAmount.toLowerCase(Locale.ENGLISH);
        try {
            if (strAmount.contains("l")) {
                strAmount = strAmount.replaceAll("l", "");
                int neededLevel = Integer.parseInt(strAmount);
                if (give) {
                    neededLevel += target.getBase().experienceLevel;
                }
                amount = SetExpFix.getExpToLevel(neededLevel);
                SetExpFix.setTotalExperience(target.getBase(), 0);
            } else {
                amount = Long.parseLong(strAmount);
                if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
                    throw new NotEnoughArgumentsException();
                }
            }
        } catch (final NumberFormatException e) {
            throw new NotEnoughArgumentsException(e);
        }
        if (give) {
            amount += SetExpFix.getTotalExperience(target.getBase());
        }
        if (amount > Integer.MAX_VALUE) {
            amount = Integer.MAX_VALUE;
        }
        if (amount < 0L) {
            amount = 0L;
        }
        SetExpFix.setTotalExperience(target.getBase(), (int) amount);
        sender.sendTl("expSet", target.getDisplayName(), amount);
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("show"));
            for (final ExpCommands cmd : ExpCommands.values()) {
                if (cmd.hasPermission(user)) {
                    options.add(cmd.name().toLowerCase(Locale.ENGLISH));
                }
            }
            return options;
        } else if (args.length == 2) {
            final ExpCommands cmd;
            try {
                cmd = ExpCommands.valueOf(args[0].toUpperCase(Locale.ENGLISH));
            } catch (final IllegalArgumentException e) {
                return Collections.emptyList();
            }
            if (cmd.hasOtherPermission(user)) {
                return getPlayers(user);
            }
            if (cmd.hasPermission(user) && cmd != ExpCommands.SHOW && cmd != ExpCommands.RESET) {
                final String levellessArg = args[1].toLowerCase(Locale.ENGLISH).replaceAll("l", "");
                if (NumberUtil.isInt(levellessArg)) {
                    return new ArrayList<>(List.of(levellessArg + "l"));
                }
            }
        } else if (args.length == 3 && !(args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("reset"))) {
            final String levellessArg = args[2].toLowerCase(Locale.ENGLISH).replaceAll("l", "");
            if (NumberUtil.isInt(levellessArg)) {
                return new ArrayList<>(List.of(levellessArg + "l"));
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> list = new ArrayList<>();
            for (final ExpCommands cmd : ExpCommands.values()) {
                list.add(cmd.name().toLowerCase(Locale.ENGLISH));
            }
            return list;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give")) {
                final String levellessArg = args[1].toLowerCase(Locale.ENGLISH).replace("l", "");
                if (NumberUtil.isInt(levellessArg)) {
                    return new ArrayList<>(List.of(levellessArg, args[1] + "l"));
                } else {
                    return Collections.emptyList();
                }
            } else {
                return getPlayers(sender);
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }

    private enum ExpCommands {
        SET,
        GIVE,
        TAKE,
        RESET,
        SHOW(false);

        private final boolean permCheck;

        ExpCommands() {
            permCheck = true;
        }

        ExpCommands(final boolean perm) {
            permCheck = perm;
        }

        boolean hasPermission(final IUser user) {
            return user == null || !permCheck || user.isAuthorized("essentials.exp." + name().toLowerCase(Locale.ENGLISH));
        }

        boolean hasOtherPermission(final IUser user) {
            return user == null || user.isAuthorized("essentials.exp." + name().toLowerCase(Locale.ENGLISH) + ".others");
        }
    }
}
