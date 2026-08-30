package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsToggleCommand;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.minecraft.server.MinecraftServer;

public class Commandsocialspy extends EssentialsToggleCommand {
    public Commandsocialspy() {
        super("socialspy", "essentials.socialspy.others");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        toggleOtherPlayers(server, sender, args);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        handleToggleWithArgs(server, user, args);
    }

    @Override
    protected void togglePlayer(final CommandSource sender, final User user, Boolean enabled) {
        if (enabled == null) {
            enabled = !user.isSocialSpyEnabled();
        }
        user.setSocialSpyEnabled(enabled);
        user.sendTl("socialSpy", user.getDisplayName(), CommonPlaceholders.enableDisable(user.getSource(), enabled));
        if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
            sender.sendTl("socialSpy", user.getDisplayName(), CommonPlaceholders.enableDisable(user.getSource(), enabled));
        }
    }
}
