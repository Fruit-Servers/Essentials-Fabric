package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Commandclearinventory extends EssentialsCommand {
    private static final int EXTENDED_CAP = 8;

    public Commandclearinventory() {
        super("clearinventory");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        parseCommand(server, user.getSource(), commandLabel, args, user.isAuthorized("essentials.clearinventory.others"),
            user.isAuthorized("essentials.clearinventory.all") || user.isAuthorized("essentials.clearinventory.multiple"));
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        parseCommand(server, sender, commandLabel, args, true, true);
    }

    private void parseCommand(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args, final boolean allowOthers, final boolean allowAll) throws Exception {
        Collection<ServerPlayer> players = new ArrayList<>();
        final User senderUser = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        String previousClearCommand = "";
        int offset = 0;
        if (sender.isPlayer()) {
            players.add(sender.getPlayer());
            previousClearCommand = senderUser.getConfirmingClearCommand();
            senderUser.setConfirmingClearCommand(null);
        }
        if (allowAll && args.length > 0 && args[0].contentEquals("*")) {
            sender.sendTl("inventoryClearingFromAll");
            offset = 1;
            players = new ArrayList<>(ess.getOnlinePlayers());
        } else if (allowOthers && args.length > 0 && args[0].trim().length() > 2) {
            offset = 1;
            players = ess.matchPlayers(args[0].trim());
        }
        if (players.size() < 1) {
            throw new PlayerNotFoundException();
        }
        final String formattedCommand = formatCommand(commandLabel, args);
        if (senderUser != null && senderUser.isPromptingClearConfirm()) {
            if (!formattedCommand.equals(previousClearCommand)) {
                senderUser.setConfirmingClearCommand(formattedCommand);
                senderUser.sendTl("confirmClear", formattedCommand);
                return;
            }
        }
        for (final ServerPlayer player : players) {
            clearHandler(sender, player, args, offset, players.size() < EXTENDED_CAP);
        }
    }

    protected void clearHandler(final CommandSource sender, final ServerPlayer player, final String[] args, final int offset, final boolean showExtended) throws TranslatableException {
        ClearHandlerType type = ClearHandlerType.ALL_EXCEPT_ARMOR;
        final Set<Item> items = new HashSet<>();
        int amount = -1;
        if (args.length > (offset + 1) && NumberUtil.isInt(args[offset + 1])) {
            amount = Integer.parseInt(args[offset + 1]);
        }
        if (args.length > offset) {
            if (args[offset].equalsIgnoreCase("**")) {
                type = ClearHandlerType.ALL_INCLUDING_ARMOR;
            } else if (!args[offset].equalsIgnoreCase("*")) {
                final String[] split = args[offset].split(",");
                for (final String item : split) {
                    try {
                        items.add(ess.getItemDb().get(item).getItem());
                    } catch (final Exception ignored) {
                    }
                }
                type = ClearHandlerType.SPECIFIC_ITEM;
            }
        }
        final String displayName = ess.getUser(player).getDisplayName();
        if (type != ClearHandlerType.SPECIFIC_ITEM) {
            final boolean armor = type == ClearHandlerType.ALL_INCLUDING_ARMOR;
            if (showExtended) {
                sender.sendTl(armor ? "inventoryClearingAllArmor" : "inventoryClearingAllItems", displayName);
            }
            Inventories.removeItems(player, item -> true, armor);
        } else {
            for (final Item item : items) {
                final String itemName = UserData.itemKey(item).toLowerCase(Locale.ENGLISH);
                if (amount < -1) {
                    throw new TranslatableException("cannotRemoveNegativeItems");
                }
                if (amount == -1) {
                    final int removedAmount = Inventories.count(player, stack -> stack.is(item));
                    Inventories.removeItems(player, stack -> stack.is(item), true);
                    if (removedAmount > 0 || showExtended) {
                        sender.sendTl("inventoryClearingStack", removedAmount, itemName, displayName);
                    }
                } else {
                    final int available = Inventories.count(player, stack -> stack.is(item));
                    if (available >= amount) {
                        int remaining = amount;
                        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                            final ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(item)) {
                                final int take = Math.min(remaining, stack.getCount());
                                stack.shrink(take);
                                remaining -= take;
                            }
                        }
                        Inventories.update(player);
                        sender.sendTl("inventoryClearingStack", amount, itemName, displayName);
                    } else {
                        if (showExtended) {
                            sender.sendTl("inventoryClearFail", displayName, amount, itemName);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (user.isAuthorized("essentials.clearinventory.others")) {
            if (args.length == 1) {
                final List<String> options = getPlayers(user);
                if (user.isAuthorized("essentials.clearinventory.all") || user.isAuthorized("essentials.clearinventory.multiple")) {
                    options.add("*");
                }
                return options;
            } else if (args.length == 2) {
                final List<String> items = new ArrayList<>(getItems());
                items.add("*");
                items.add("**");
                return items;
            } else {
                return Collections.emptyList();
            }
        } else {
            if (args.length == 1) {
                final List<String> items = new ArrayList<>(getItems());
                items.add("*");
                items.add("**");
                return items;
            } else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = getPlayers(sender);
            options.add("*");
            return options;
        } else if (args.length == 2) {
            final List<String> items = new ArrayList<>(getItems());
            items.add("*");
            items.add("**");
            return items;
        } else {
            return Collections.emptyList();
        }
    }

    private String formatCommand(final String commandLabel, final String[] args) {
        if (args == null || args.length == 0) {
            return "/" + commandLabel;
        }
        return "/" + commandLabel + " " + StringUtil.joinList(" ", (Object[]) args);
    }

    private enum ClearHandlerType {
        ALL_EXCEPT_ARMOR, ALL_INCLUDING_ARMOR, SPECIFIC_ITEM
    }
}
