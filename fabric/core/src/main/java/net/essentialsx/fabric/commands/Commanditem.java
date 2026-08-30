package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commanditem extends EssentialsCommand {
    public Commanditem() {
        super("item");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        ItemStack stack = ess.getItemDb().get(args[0]);
        final String itemname = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace("_", "");
        if (!user.canSpawnItem(stack.getItem())) {
            throw new TranslatableException("cantSpawnItem", itemname);
        }
        try {
            if (args.length > 1 && Integer.parseInt(args[1]) > 0) {
                stack.setCount(Integer.parseInt(args[1]));
            } else if (ess.getSettings().getDefaultStackSize() > 0) {
                stack.setCount(ess.getSettings().getDefaultStackSize());
            } else if (ess.getSettings().getOversizedStackSize() > 0 && user.isAuthorized("essentials.oversizedstacks")) {
                stack.setCount(ess.getSettings().getOversizedStackSize());
            }
        } catch (final NumberFormatException e) {
            throw new NotEnoughArgumentsException();
        }
        final MetaItemStack metaStack = new MetaItemStack(stack);
        if (!metaStack.canSpawn(ess)) {
            throw new TranslatableException("unableToSpawnItem", itemname);
        }
        if (args.length > 2) {
            final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && user.isAuthorized("essentials.enchantments.allowunsafe");
            metaStack.parseStringMeta(user.getSource(), allowUnsafe, args, 2, ess);
            stack = metaStack.getItemStack();
        }
        if (stack.isEmpty()) {
            throw new TranslatableException("cantSpawnItem", "Air");
        }
        final String displayName = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace('_', ' ');
        user.sendTl("itemSpawn", stack.getCount(), displayName);
        Inventories.addItem(user.getBase(), user.isAuthorized("essentials.oversizedstacks") ? ess.getSettings().getOversizedStackSize() : 0, false, stack);
        Inventories.update(user.getBase());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("1", "64"));
        } else if (args.length == 3) {
            return new ArrayList<>(List.of("0"));
        } else {
            return Collections.emptyList();
        }
    }
}
