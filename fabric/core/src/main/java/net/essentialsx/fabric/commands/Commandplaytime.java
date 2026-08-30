package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Commandplaytime extends EssentialsCommand {
    public Commandplaytime() {
        super("playtime");
    }

    /** Vanilla play-time statistic (ticks) for an offline player, read from the world's stats folder. */
    public static long offlinePlaytimeTicks(final MinecraftServer server, final UUID uuid) {
        final File file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json").toFile();
        if (!file.exists()) {
            return 0;
        }
        final ServerStatsCounter counter = new ServerStatsCounter(server, file);
        return counter.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        String displayName;
        long playtime;
        final String key;
        if (args.length > 0 && sender.isAuthorized("essentials.playtime.others")) {
            try {
                final IUser user = getPlayer(server, sender, args, 0);
                displayName = user.getDisplayName();
                playtime = user.getBase().getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            } catch (final PlayerNotFoundException e) {
                final User user = getPlayer(server, args, 0, true, true);
                displayName = user.getName(); // Vanished players will have their name as their display name
                if (user.isOnline()) {
                    playtime = user.getBase().getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                } else {
                    playtime = offlinePlaytimeTicks(server, user.getUUID());
                }
                if (user.isOnline() && user.isVanished()) {
                    playtime = playtime - ((System.currentTimeMillis() - user.getLastVanishTime()) / 50L);
                }
            }
            key = "playtimeOther";
        } else if (sender.isPlayer()) {
            displayName = sender.getUser().getDisplayName();
            playtime = sender.getPlayer().getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            key = "playtime";
        } else {
            throw new NotEnoughArgumentsException();
        }
        final long playtimeMs = System.currentTimeMillis() - (playtime * 50L);
        sender.sendTl(key, DateUtil.formatDateDiff(playtimeMs), Text.parsed(Text.get().legacyToMini(displayName)));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.playtime.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
