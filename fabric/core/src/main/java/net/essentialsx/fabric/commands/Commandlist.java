package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.PlayerList;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandlist extends EssentialsCommand {
    public Commandlist() {
        super("list");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        boolean showHidden = true;
        User user = null;
        if (sender.isPlayer()) {
            user = ess.getUser(sender.getPlayer());
            showHidden = user.isAuthorized("essentials.list.hidden") || user.canInteractVanished();
        }
        sender.sendComponent(Text.get().deserializeMiniMessage(PlayerList.listSummary(ess, user, showHidden)));
        final Map<String, List<User>> playerList = PlayerList.getPlayerLists(ess, user, showHidden);
        if (args.length > 0) {
            sender.sendComponent(Text.get().deserializeMiniMessage(PlayerList.listGroupUsers(ess, playerList, args[0].toLowerCase(Locale.ENGLISH))));
        } else {
            sendGroupedList(sender, commandLabel, playerList);
        }
    }

    // Output the standard /list output, when no group is specified
    private void sendGroupedList(final CommandSource sender, final String commandLabel, final Map<String, List<User>> playerList) {
        for (final String str : PlayerList.prepareGroupedList(ess, sender, commandLabel, playerList)) {
            sender.sendComponent(Text.get().deserializeMiniMessage(str));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(PlayerList.getPlayerLists(ess, sender.getUser(), false).keySet());
        } else {
            return Collections.emptyList();
        }
    }
}
