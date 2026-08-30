package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

public class Commandsetworth extends EssentialsCommand {
    public Commandsetworth() {
        super("setworth");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final ItemStack stack;
        final String price;
        if (args.length == 1) {
            stack = user.getItemInHand();
            price = args[0];
        } else {
            stack = ess.getItemDb().get(args[0]);
            price = args[1];
        }
        ess.getWorth().setPrice(ess, stack, Double.parseDouble(price));
        user.sendTl("worthSet");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        ess.getWorth().setPrice(ess, ess.getItemDb().get(args[0]), Double.parseDouble(args[1]));
        sender.sendTl("worthSet");
    }
}
