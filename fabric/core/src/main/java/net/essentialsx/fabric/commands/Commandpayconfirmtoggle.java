package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandpayconfirmtoggle extends EssentialsCommand {
    public Commandpayconfirmtoggle() {
        super("payconfirmtoggle");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        boolean confirmingPay = !user.isPromptingPayConfirm();
        if (commandLabel.contains("payconfirmon")) {
            confirmingPay = true;
        } else if (commandLabel.contains("payconfirmoff")) {
            confirmingPay = false;
        }
        user.setPromptingPayConfirm(confirmingPay);
        user.sendTl(confirmingPay ? "payConfirmToggleOn" : "payConfirmToggleOff");
        user.getConfirmingPayments().clear();
    }
}
