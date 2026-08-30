package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Commandwarp extends EssentialsCommand {
    private static final int WARPS_PER_PAGE = 20;

    public Commandwarp() {
        super("warp");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || args[0].matches("[0-9]+")) {
            if (!user.isAuthorized("essentials.warp.list")) {
                throw new TranslatableException("warpListPermission");
            }
            warpList(user.getSource(), args, user);
            throw new NoChargeException();
        }
        if (args.length == 2 && (user.isAuthorized("essentials.warp.otherplayers") || user.isAuthorized("essentials.warp.others"))) {
            final User otherUser = getPlayer(server, user, args, 1);
            warpUser(user, otherUser, args[0], commandLabel);
            throw new NoChargeException();
        }
        warpUser(user, user, args[0], commandLabel);
        throw new NoChargeException();
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2 || NumberUtil.isInt(args[0])) {
            warpList(sender, args, null);
            throw new NoChargeException();
        }
        final User otherUser = getPlayer(server, args, 1, true, false);
        otherUser.getAsyncTeleport().warp(otherUser, args[0], null, TeleportCause.COMMAND, getNewExceptionFuture(sender, commandLabel));
        throw new NoChargeException();
    }

    private void warpList(final CommandSource sender, final String[] args, final IUser user) throws Exception {
        final List<String> warpNameList = getAvailableWarpsFor(user);
        if (warpNameList.isEmpty()) {
            throw new TranslatableException("noWarpsDefined");
        }
        int page = 1;
        if (args.length > 0 && NumberUtil.isInt(args[0])) {
            page = Integer.parseInt(args[0]);
        }
        final int maxPages = (int) Math.ceil(warpNameList.size() / (double) WARPS_PER_PAGE);
        if (page > maxPages) {
            page = maxPages;
        }
        if (page < 1) {
            page = 1;
        }
        final int warpPage = (page - 1) * WARPS_PER_PAGE;
        final String warpList = StringUtil.joinList(warpNameList.subList(warpPage, warpPage + Math.min(warpNameList.size() - warpPage, WARPS_PER_PAGE)).toArray());
        if (warpNameList.size() > WARPS_PER_PAGE) {
            sender.sendTl("warpsCount", warpNameList.size(), page, maxPages);
            sender.sendTl("warpList", warpList);
        } else {
            sender.sendTl("warps", warpList);
        }
    }

    private void warpUser(final User owner, final User user, final String name, final String commandLabel) throws Exception {
        final Trade chargeWarp = new Trade("warp-" + name.toLowerCase(Locale.ENGLISH).replace('_', '-'), ess);
        final Trade chargeCmd = new Trade(this.getName(), ess);
        final BigDecimal fullCharge = chargeWarp.getCommandCost(user).add(chargeCmd.getCommandCost(user));
        final Trade charge = new Trade(fullCharge, ess);
        charge.isAffordableFor(owner);
        if (ess.getSettings().getPerWarpPermission() && !owner.isAuthorized("essentials.warps." + name)) {
            throw new TranslatableException("warpUsePermission");
        }
        owner.getAsyncTeleport().warp(user, name, charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
    }

    private List<String> getAvailableWarpsFor(final IUser user) {
        if (ess.getSettings().getPerWarpPermission() && user != null) {
            return ess.getWarps().getList().stream()
                .filter(warpName -> user.isAuthorized("essentials.warps." + warpName))
                .collect(Collectors.toList());
        }
        return new ArrayList<>(ess.getWarps().getList());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.warp.list")) {
            return getAvailableWarpsFor(user);
        } else if (args.length == 2 && (user.isAuthorized("essentials.warp.otherplayers") || user.isAuthorized("essentials.warp.others"))) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getWarps().getList());
        } else if (args.length == 2) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
