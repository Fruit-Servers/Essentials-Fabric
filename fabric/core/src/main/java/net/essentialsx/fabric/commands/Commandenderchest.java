package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandenderchest extends EssentialsCommand {
    public Commandenderchest() {
        super("enderchest");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        User target = user;
        if (args.length > 0 && user.isAuthorized("essentials.enderchest.others")) {
            target = getPlayer(server, user, args, 0);
        }
        user.getBase().closeContainer();
        final boolean readOnly = !target.equals(user) && !user.isAuthorized("essentials.enderchest.modify");
        Workstations.openEnderChest(user.getBase(), target.getBase(), readOnly);
        user.setEnderSee(!target.equals(user));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.enderchest.others")) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }
}
