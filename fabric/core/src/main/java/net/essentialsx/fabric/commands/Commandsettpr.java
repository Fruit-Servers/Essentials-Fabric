package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.rtp.RandomTeleport;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandsettpr extends EssentialsCommand {
    public Commandsettpr() {
        super("settpr");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        if ("center".equalsIgnoreCase(args[1])) {
            randomTeleport.setCenter(args[0], user.getLocation());
            user.sendTl("settpr");
        } else if (args.length > 2) {
            if ("minrange".equalsIgnoreCase(args[1])) {
                randomTeleport.setMinRange(args[0], Double.parseDouble(args[2]));
            } else if ("maxrange".equalsIgnoreCase(args[1])) {
                randomTeleport.setMaxRange(args[0], Double.parseDouble(args[2]));
            }
            user.sendTl("settprValue", args[1].toLowerCase(Locale.ENGLISH), args[2].toLowerCase(Locale.ENGLISH));
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getWorlds(server);
        } else if (args.length == 2) {
            return Arrays.asList("center", "minrange", "maxrange");
        }
        return Collections.emptyList();
    }
}
