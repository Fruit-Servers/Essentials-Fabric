package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandpowertoollist extends EssentialsCommand {
    public Commandpowertoollist() {
        super("powertoollist");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.hasPowerTools()) {
            user.sendTl("noPowerTools");
            return;
        }
        final Map<String, List<String>> powertools = user.getAllPowertools();
        for (final Map.Entry<String, List<String>> entry : powertools.entrySet()) {
            final String itemName = entry.getKey().toLowerCase(Locale.ENGLISH).replaceAll("_", " ");
            final List<String> commands = entry.getValue();
            user.sendTl("powerToolList", StringUtil.joinList(commands.toArray()), itemName);
        }
    }
}
