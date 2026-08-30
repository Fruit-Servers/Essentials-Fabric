package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commanddisposal extends EssentialsCommand {
    public Commanddisposal() {
        super("disposal");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        user.sendTl("openingDisposal");
        Workstations.openDisposal(user.getBase(), Text.get().legacy(user.playerTl("disposal")));
    }
}
