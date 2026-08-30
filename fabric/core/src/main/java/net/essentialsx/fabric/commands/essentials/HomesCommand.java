package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HomesCommand extends EssentialsTreeNode {
    private static final String HOMES_USAGE = "/<command> homes (fix | delete [world])";

    public HomesCommand() {
        super("homes");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            sender.sendMessage("This sub-command provides a utility to mass-delete homes based on user options:");
            sender.sendMessage("Use \"fix\" to delete all homes inside non-existent or unloaded worlds.");
            sender.sendMessage("Use \"delete\" to delete all existing homes.");
            sender.sendMessage("Use \"delete <worldname>\" to delete all homes inside a specific world.");
            throw new Exception(HOMES_USAGE);
        }
        switch (args[0]) {
            case "fix":
                sender.sendTl("fixingHomes");
                ess.runTaskAsynchronously(() -> {
                    for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                        final User user = ess.getUsers().getUser(u);
                        if (user == null) {
                            continue;
                        }
                        for (final String homeName : new ArrayList<>(user.getHomes())) {
                            try {
                                final LazyLocation home = user.getHome(homeName);
                                if (home == null || !home.isAvailable(ess.getServer())) {
                                    user.delHome(homeName);
                                }
                            } catch (final Exception e) {
                                ess.getLogger().info("Unable to delete home " + homeName + " for " + user.getName());
                            }
                        }
                    }
                    sender.sendTl("fixedHomes");
                });
                break;
            case "delete":
                final boolean filterByWorld = args.length >= 2;
                final ServerLevel world = filterByWorld ? Worlds.get(ess.getServer(), args[1]) : null;
                if (filterByWorld && world == null) {
                    throw new TranslatableException("invalidWorld");
                }
                if (filterByWorld) {
                    sender.sendTl("deletingHomesWorld", args[1]);
                } else {
                    sender.sendTl("deletingHomes");
                }
                final String worldKey = world == null ? null : Worlds.key(world);
                ess.runTaskAsynchronously(() -> {
                    for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                        final User user = ess.getUsers().getUser(u);
                        if (user == null) {
                            continue;
                        }
                        for (final String homeName : new ArrayList<>(user.getHomes())) {
                            try {
                                final LazyLocation home = user.getHome(homeName);
                                if (!filterByWorld || home != null && worldKey.equals(home.world())) {
                                    user.delHome(homeName);
                                }
                            } catch (final Exception e) {
                                ess.getLogger().info("Unable to delete home " + homeName + " for " + user.getName());
                            }
                        }
                    }
                    if (filterByWorld) {
                        sender.sendTl("deletedHomesWorld", args[1]);
                    } else {
                        sender.sendTl("deletedHomes");
                    }
                });
                break;
            default:
                throw new Exception(HOMES_USAGE);
        }
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("fix", "delete"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return Worlds.names(ess.getServer());
        }
        return Collections.emptyList();
    }
}
