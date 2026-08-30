package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

public class Commandunlimited extends EssentialsCommand {
    public Commandunlimited() {
        super("unlimited");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        User target = user;
        if (args.length > 1 && user.isAuthorized("essentials.unlimited.others")) {
            target = getPlayer(server, user, args, 1);
        }
        if (args[0].equalsIgnoreCase("list")) {
            user.sendComponent(Text.get().deserializeMiniMessage(getList(user, target)));
        } else if (args[0].equalsIgnoreCase("clear")) {
            for (final String m : new HashSet<>(target.getUnlimited())) {
                if (m == null) {
                    continue;
                }
                toggleUnlimited(user, target, m);
            }
        } else {
            toggleUnlimited(user, target, args[0]);
        }
    }

    private String getList(final User sendTo, final User target) {
        final StringBuilder output = new StringBuilder();
        output.append(sendTo.playerTl("unlimitedItems")).append(" ");
        final Set<String> items = target.getUnlimited();
        if (items.isEmpty()) {
            output.append(sendTo.playerTl("none"));
        }
        final StringJoiner joiner = new StringJoiner(", ");
        for (final String material : items) {
            if (material == null) {
                continue;
            }
            joiner.add(material.toLowerCase(Locale.ENGLISH).replace("_", ""));
        }
        output.append(joiner);
        return output.toString();
    }

    private void toggleUnlimited(final User user, final User target, final String item) throws Exception {
        final ItemStack stack = ess.getItemDb().get(item, 1);
        stack.setCount(Math.min(stack.getMaxStackSize(), 2));
        final String itemname = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace("_", "");
        if (ess.getSettings().permissionBasedItemSpawn() && !user.isAuthorized("essentials.unlimited.item-all") && !user.isAuthorized("essentials.unlimited.item-" + itemname) && !((stack.getItem() == Items.WATER_BUCKET || stack.getItem() == Items.LAVA_BUCKET) && user.isAuthorized("essentials.unlimited.item-bucket"))) {
            throw new TranslatableException("unlimitedItemPermission", itemname);
        }
        String message = "disableUnlimited";
        boolean enableUnlimited = false;
        if (!target.hasUnlimited(stack)) {
            message = "enableUnlimited";
            enableUnlimited = true;
            if (!Inventories.containsAtLeast(target.getBase(), stack, stack.getCount())) {
                Inventories.addItem(target.getBase(), stack);
                Inventories.update(target.getBase());
            }
        }
        if (user != target) {
            user.sendTl(message, itemname, target.getDisplayName());
        }
        target.sendTl(message, itemname, target.getDisplayName());
        target.setUnlimited(stack, enableUnlimited);
    }
}
