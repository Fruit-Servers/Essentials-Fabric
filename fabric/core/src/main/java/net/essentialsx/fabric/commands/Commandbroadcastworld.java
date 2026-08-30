package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.List;

public class Commandbroadcastworld extends EssentialsCommand {
    public Commandbroadcastworld() {
        super("broadcastworld");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        ServerLevel world = user.getWorld();
        String message = getFinalArg(args, 0);
        if (ess.getSettings().isAllowWorldInBroadcastworld()) {
            final ServerLevel argWorld = Worlds.get(server, args[0]);
            if (argWorld != null) {
                world = argWorld;
                message = getFinalArg(args, 1);
            }
        }
        sendBroadcast(world, user.getDisplayName(), message);
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final ServerLevel world = Worlds.get(server, args[0]);
        if (world == null) {
            throw new TranslatableException("invalidWorld");
        }
        sendBroadcast(world, sender.getName(), getFinalArg(args, 1));
    }

    private void sendBroadcast(final ServerLevel world, final String name, final String message) throws Exception {
        if (message.isEmpty()) {
            throw new NotEnoughArgumentsException();
        }
        final String formatted = FormatUtil.replaceFormat(message).replace("\\n", "\n");
        ess.broadcastTl(null, u -> u.getBase() == null || u.getBase().level() != world, "broadcast",
            Text.parsed(Text.get().legacyToMiniWithUrls(Text.get().escapeTags(formatted))),
            Text.parsed(Text.get().legacyToMini(name)));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && (!sender.isPlayer() || ess.getSettings().isAllowWorldInBroadcastworld())) {
            return Worlds.names(server);
        } else {
            return Collections.emptyList();
        }
    }
}
