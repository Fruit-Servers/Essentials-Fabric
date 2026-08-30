package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandclearinventoryconfirmtoggle extends EssentialsCommand {
    public Commandclearinventoryconfirmtoggle() {
        super("clearinventoryconfirmtoggle");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        boolean confirmingClear = !user.isPromptingClearConfirm();
        if (commandLabel.toLowerCase().endsWith("on")) {
            confirmingClear = true;
        } else if (commandLabel.toLowerCase().endsWith("off")) {
            confirmingClear = false;
        }
        user.setPromptingClearConfirm(confirmingClear);
        if (confirmingClear) {
            user.sendTl("clearInventoryConfirmToggleOn");
        } else {
            user.sendTl("clearInventoryConfirmToggleOff");
        }
        user.setConfirmingClearCommand(null);
    }
}
