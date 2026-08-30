package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public class Commandkickall extends EssentialsCommand {
    public Commandkickall() {
        super("kickall");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        String kickReason = args.length > 0 ? getFinalArg(args, 0) : Text.get().miniToLegacy(I18n.tlLiteral("kickDefault"));
        kickReason = FormatUtil.replaceFormat(kickReason.replace("\\n", "\n").replace("|", "\n"));
        for (final ServerPlayer onlinePlayer : new ArrayList<>(ess.getOnlinePlayers())) {
            if (!sender.isPlayer() || !onlinePlayer.getGameProfile().getName().equalsIgnoreCase(sender.getPlayer().getGameProfile().getName())) {
                final User user = ess.getUser(onlinePlayer);
                if (!user.isAuthorized("essentials.kickall.exempt")) {
                    ess.kickPlayer(user, kickReason);
                }
            }
        }
        sender.sendTl("kickedAll");
    }
}
