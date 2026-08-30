package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.essentialsx.fabric.user.UserMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UsermapCommand extends EssentialsTreeNode {
    public UsermapCommand() {
        super("usermap");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (!sender.isAuthorized("essentials.usermap")) {
            return;
        }
        final UserMap userMap = ess.getUsers();
        sender.sendTl("usermapSize", userMap.getOnlineUserCache().size(), userMap.getUserCount(), ess.getSettings().getMaxUserCacheCount());
        if (args.length > 0) {
            if (args[0].equals("full")) {
                for (final Map.Entry<String, UUID> entry : userMap.getNameCache().entrySet()) {
                    sender.sendTl("usermapEntry", entry.getKey(), entry.getValue().toString());
                }
            } else if (args[0].equals("purge")) {
                final boolean seppuku = args.length > 1 && args[1].equals("iknowwhatimdoing");
                sender.sendTl("usermapPurge", String.valueOf(seppuku));
                final Set<UUID> uuids = new HashSet<>(userMap.getAllUserUUIDs());
                ess.runTaskAsynchronously(() -> {
                    final File userdataFolder = userMap.getUserdataFolder().toFile();
                    final File backupFolder = new File(ess.getDataFolder().toFile(), "userdata-npc-backup-boogaloo-" + System.currentTimeMillis());
                    if (!userdataFolder.isDirectory()) {
                        ess.getLogger().warn("Missing userdata folder, aborting usermap purge.");
                        return;
                    }
                    if (seppuku && !backupFolder.mkdir()) {
                        ess.getLogger().warn("Unable to create backup folder, aborting usermap purge.");
                        return;
                    }
                    int total = 0;
                    final File[] files = userdataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
                    if (files != null) {
                        for (final File file : files) {
                            try {
                                final String fileName = file.getName();
                                final UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                                if (!uuids.contains(uuid)) {
                                    total++;
                                    ess.getLogger().warn("Found orphaned userdata file: " + file.getName());
                                    if (seppuku) {
                                        try {
                                            Files.move(file.toPath(), new File(backupFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                        } catch (final IOException e) {
                                            ess.getLogger().warn("Unable to move orphaned userdata file: " + file.getName(), e);
                                        }
                                    }
                                }
                            } catch (final IllegalArgumentException ignored) {
                            }
                        }
                    }
                    ess.getLogger().info("Found " + total + " orphaned userdata files.");
                });
            } else if (args[0].equalsIgnoreCase("cache")) {
                sender.sendTl("usermapKnown", userMap.getAllUserUUIDs().size(), userMap.getNameCache().size());
            } else {
                try {
                    final UUID uuid = UUID.fromString(args[0]);
                    for (final Map.Entry<String, UUID> entry : userMap.getNameCache().entrySet()) {
                        if (entry.getValue().equals(uuid)) {
                            sender.sendTl("usermapEntry", entry.getKey(), args[0]);
                        }
                    }
                } catch (final IllegalArgumentException ignored) {
                    final String sanitizedName = args[0].toLowerCase(Locale.ENGLISH);
                    final UUID uuid = userMap.getNameCache().get(sanitizedName);
                    sender.sendTl("usermapEntry", sanitizedName, uuid == null ? "null" : uuid.toString());
                }
            }
        }
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("full", "purge", "cache"));
        }
        return Collections.emptyList();
    }
}
