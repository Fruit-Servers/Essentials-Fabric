package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class Commandtpoffline extends EssentialsCommand {
    public Commandtpoffline() {
        super("tpoffline");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String label, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        } else {
            final User target = getPlayer(server, args, 0, true, true);
            final LazyLocation logout = target.getLogoutLocation();
            if (logout == null) {
                user.sendTl("teleportOfflineUnknown", target.getDisplayName());
                throw new NoChargeException();
            }
            final ServerLevel level = logout.level(server);
            final String worldName = level == null ? logout.worldDisplayName(server) : Worlds.permissionName(level);
            if (!logout.sameWorld(user.getLocation()) && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + worldName)) {
                throw new TranslatableException("noPerm", "essentials.worlds." + worldName);
            }
            user.sendTl("teleporting", logout.worldDisplayName(server), logout.blockX(), logout.blockY(), logout.blockZ());
            user.getAsyncTeleport().now(logout, false, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), label));
        }
    }
}
