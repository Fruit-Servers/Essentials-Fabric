package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandpowertool extends EssentialsCommand {
    public Commandpowertool() {
        super("powertool");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final String command = getFinalArg(args, 0);
        final ItemStack itemStack = Inventories.getItemInHand(user.getBase());
        powertool(user.getSource(), user, itemStack, command);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 3) {
            throw new Exception("When running from console, usage is: /" + commandLabel + " <player> <itemid> <command>");
        }
        final User user = getPlayer(server, args, 0, true, true);
        final ItemStack itemStack = ess.getItemDb().get(args[1]);
        final String command = getFinalArg(args, 2);
        powertool(sender, user, itemStack, command);
    }

    protected void powertool(final CommandSource sender, final User user, final ItemStack itemStack, String command) throws Exception {
        // check to see if this is a clear all command
        if (command != null && command.equalsIgnoreCase("d:")) {
            user.clearAllPowertools();
            sender.sendTl("powerToolClearAll");
            return;
        }
        if (itemStack == null || itemStack.isEmpty()) {
            throw new TranslatableException("powerToolAir");
        }
        final String itemName = UserData.itemKey(itemStack).toLowerCase(Locale.ENGLISH).replaceAll("_", " ");
        final List<String> powertools = user.getPowertool(itemStack) != null ? new ArrayList<>(user.getPowertool(itemStack)) : new ArrayList<>();
        if (command != null && !command.isEmpty()) {
            if (command.equalsIgnoreCase("l:")) {
                if (powertools.isEmpty()) {
                    throw new TranslatableException("powerToolListEmpty", itemName);
                } else {
                    sender.sendTl("powerToolList", StringUtil.joinList(powertools.toArray()), itemName);
                }
                throw new NoChargeException();
            }
            if (command.startsWith("r:")) {
                command = command.substring(2);
                if (!powertools.contains(command)) {
                    throw new TranslatableException("powerToolNoSuchCommandAssigned", command, itemName);
                }
                powertools.remove(command);
                sender.sendTl("powerToolRemove", command, itemName);
            } else {
                if (command.startsWith("a:")) {
                    if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.powertool.append")) {
                        throw new TranslatableException("noPerm", "essentials.powertool.append");
                    }
                    command = command.substring(2);
                    if (powertools.contains(command)) {
                        throw new TranslatableException("powerToolAlreadySet", command, itemName);
                    }
                } else if (!powertools.isEmpty()) {
                    // Replace all commands with this one
                    powertools.clear();
                }
                powertools.add(command);
                sender.sendTl("powerToolAttach", StringUtil.joinList(powertools.toArray()), itemName);
            }
        } else {
            powertools.clear();
            sender.sendTl("powerToolRemoveAll", itemName);
        }
        if (!user.arePowerToolsEnabled()) {
            user.setPowerToolsEnabled(true);
            user.sendTl("powerToolsEnabled");
        }
        user.setPowertool(itemStack, powertools);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("d:", "c:", "l:"));
            if (user.isAuthorized("essentials.powertool.append")) {
                for (final String command : getCommands(server)) {
                    options.add("a:" + command);
                }
            }
            try {
                final ItemStack itemStack = Inventories.getItemInHand(user.getBase());
                final List<String> powertools = user.getPowertool(itemStack);
                for (final String tool : powertools) {
                    options.add("r:" + tool);
                }
            } catch (final Exception ignored) {
            }
            return options;
        } else if (args[0].startsWith("a:")) {
            return tabCompleteCommand(user.getSource(), server, args[0].substring(2), args, 1);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            return getItems();
        } else if (args.length == 3) {
            final List<String> options = new ArrayList<>(List.of("d:", "c:", "l:"));
            for (final String command : getCommands(server)) {
                options.add("a:" + command);
            }
            try {
                final User user = getPlayer(server, args, 0, true, true);
                final ItemStack itemStack = ess.getItemDb().get(args[1]);
                final List<String> powertools = user.getPowertool(itemStack);
                for (final String tool : powertools) {
                    options.add("r:" + tool);
                }
            } catch (final Exception ignored) {
            }
            return options;
        } else if (args[2].startsWith("a:")) {
            return tabCompleteCommand(sender, server, args[2].substring(2), args, 3);
        } else {
            return Collections.emptyList();
        }
    }
}
