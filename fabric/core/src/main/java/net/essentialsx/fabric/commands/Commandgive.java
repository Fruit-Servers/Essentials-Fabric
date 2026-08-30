package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandgive extends EssentialsLoopCommand {
    public Commandgive() {
        super("give");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        ItemStack stack = ess.getItemDb().get(args[1]);
        final String itemname = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace("_", "");
        if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).canSpawnItem(stack.getItem())) {
            throw new TranslatableException("cantSpawnItem", itemname);
        }
        try {
            if (args.length > 2 && Integer.parseInt(args[2]) > 0) {
                stack.setCount(Integer.parseInt(args[2]));
            } else if (ess.getSettings().getDefaultStackSize() > 0) {
                stack.setCount(ess.getSettings().getDefaultStackSize());
            } else if (ess.getSettings().getOversizedStackSize() > 0 && sender.isAuthorized("essentials.oversizedstacks")) {
                stack.setCount(ess.getSettings().getOversizedStackSize());
            }
        } catch (final NumberFormatException e) {
            throw new NotEnoughArgumentsException();
        }
        final MetaItemStack metaStack = new MetaItemStack(stack);
        if (!metaStack.canSpawn(ess)) {
            throw new TranslatableException("unableToSpawnItem", itemname);
        }
        if (args.length > 3) {
            boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
            if (allowUnsafe && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.enchantments.allowunsafe")) {
                allowUnsafe = false;
            }
            final int metaStart = NumberUtil.isInt(args[3]) ? 4 : 3;
            if (args.length > metaStart) {
                metaStack.parseStringMeta(sender, allowUnsafe, args, metaStart, ess);
            }
            stack = metaStack.getItemStack();
        }
        if (stack.isEmpty()) {
            throw new TranslatableException("cantSpawnItem", "Air");
        }
        final String itemName = UserData.itemKey(stack).toLowerCase(Locale.ENGLISH).replace('_', ' ');
        final boolean isDropItemsIfFull = ess.getSettings().isDropItemsIfFull();
        final ItemStack finalStack = stack;
        loopOnlinePlayersConsumer(server, sender, false, true, args[0], player -> {
            sender.sendTl("giveSpawn", finalStack.getCount(), itemName, player.getDisplayName());
            final Map<Integer, ItemStack> leftovers = Inventories.addItem(player.getBase(), player.isAuthorized("essentials.oversizedstacks") ? ess.getSettings().getOversizedStackSize() : 0, false, finalStack.copy());
            for (final ItemStack item : leftovers.values()) {
                if (isDropItemsIfFull) {
                    Inventories.dropNaturally(player.getBase(), item);
                } else {
                    sender.sendTl("giveSpawnFailure", item.getCount(), itemName, player.getDisplayName());
                }
            }
            Inventories.update(player.getBase());
        });
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            return getItems();
        } else if (args.length == 3) {
            return new ArrayList<>(List.of("1", "64"));
        } else if (args.length == 4) {
            return new ArrayList<>(List.of("0"));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
    }
}
