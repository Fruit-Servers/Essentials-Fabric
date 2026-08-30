package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.WarpNotFoundException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.essentialsx.fabric.warp.Warps;
import net.minecraft.server.MinecraftServer;

public class Commandsetwarp extends EssentialsCommand {
    public Commandsetwarp() {
        super("setwarp");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        if (NumberUtil.isInt(args[0]) || args[0].isEmpty()) {
            throw new TranslatableException("invalidWarpName");
        }
        if (StringUtil.isReservedFileName(args[0])) {
            throw new TranslatableException("invalidWarpName");
        }
        final Warps warps = ess.getWarps();
        LazyLocation warpLoc = null;
        try {
            warpLoc = warps.getWarp(args[0]);
        } catch (final WarpNotFoundException ignored) {
        }
        if (warpLoc == null) {
            warps.setWarp(user, args[0], user.getLocation());
        } else if (user.isAuthorized("essentials.warp.overwrite." + StringUtil.safeString(args[0]))) {
            warps.setWarp(user, args[0], user.getLocation());
        } else {
            throw new TranslatableException("warpOverwrite");
        }
        user.sendTl("warpSet", args[0]);
    }
}
