package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commanditemdb extends EssentialsCommand {
    public Commanditemdb() {
        super("itemdb");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        ItemStack itemStack = null;
        boolean itemHeld = false;
        if (args.length == 0) {
            if (sender.isPlayer() && sender.getPlayer() != null) {
                itemHeld = true;
                itemStack = ess.getUser(sender.getPlayer()).getItemInHand();
            }
            if (itemStack == null) {
                throw new NotEnoughArgumentsException();
            }
        } else {
            itemStack = ess.getItemDb().get(args[0]);
        }
        final String itemId = "none";
        sender.sendTl("itemType", UserData.itemKey(itemStack), itemId);
        if (itemHeld && !itemStack.isEmpty()) {
            final int maxuses = itemStack.getMaxDamage();
            final int durability = (maxuses + 1) - MaterialUtil.getDamage(itemStack);
            if (maxuses != 0) {
                sender.sendTl("durability", Integer.toString(durability));
            }
        }
        List<String> nameList = ess.getItemDb().nameList(itemStack);
        nameList = nameList != null ? new ArrayList<>(nameList) : new ArrayList<>();
        if (nameList.isEmpty()) {
            return;
        }
        Collections.sort(nameList);
        if (nameList.size() > 15) {
            nameList = nameList.subList(0, 14);
        }
        final String itemNameList = StringUtil.joinList(", ", nameList.toArray());
        sender.sendTl("itemNames", itemNameList);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else {
            return Collections.emptyList();
        }
    }
}
