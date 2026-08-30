package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class Commandmore extends EssentialsCommand {
    public Commandmore() {
        super("more");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack stack = user.getItemInHand();
        if (stack == null || stack.isEmpty()) {
            throw new TranslatableException("cantSpawnItem", "Air");
        }
        final boolean canOversized = user.isAuthorized("essentials.oversizedstacks");
        if (stack.getCount() >= (canOversized ? ess.getSettings().getOversizedStackSize() : stack.getMaxStackSize())) {
            throw new TranslatableException("fullStack");
        }
        final String itemname = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace("_", "");
        if (!user.canSpawnItem(stack.getItem())) {
            throw new TranslatableException("cantSpawnItem", itemname);
        }
        int newStackSize;
        if (args.length >= 1) {
            if (!NumberUtil.isPositiveInt(args[0])) {
                throw new TranslatableException("nonZeroPosNumber");
            }
            final int cap = canOversized ? ess.getSettings().getOversizedStackSize() : stack.getMaxStackSize();
            final long newSizeLong = (long) stack.getCount() + Integer.parseInt(args[0]);
            if (newSizeLong > cap) {
                user.sendTl(canOversized ? "fullStackDefaultOversize" : "fullStackDefault", cap);
                newStackSize = cap;
            } else {
                newStackSize = (int) newSizeLong;
            }
        } else if (canOversized) {
            newStackSize = ess.getSettings().getOversizedStackSize();
        } else {
            newStackSize = stack.getMaxStackSize();
        }
        stack.setCount(newStackSize);
        Inventories.update(user.getBase());
    }
}
