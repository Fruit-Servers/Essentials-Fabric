package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandinvsee extends EssentialsCommand {
    public Commandinvsee() {
        super("invsee");
    }

    //This method has a hidden param, which if given will display the equip slots. #easteregg
    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User invUser = getPlayer(server, user, args, 0);
        if (user == invUser) {
            user.sendTl("invseeNoSelf");
            throw new NoChargeException();
        }
        final boolean readOnly = !user.isAuthorized("essentials.invsee.modify");
        final String title = args.length > 1 && user.isAuthorized("essentials.invsee.equip") ? user.playerTl("equipped") : invUser.getName();
        user.getBase().closeContainer();
        Workstations.openInvsee(user.getBase(), invUser.getBase(), readOnly, Text.get().legacy(title));
        user.setInvSee(true);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> suggestions = getPlayers(user);
            suggestions.remove(user.getName());
            return suggestions;
        } else {
            return Collections.emptyList();
        }
    }
}
