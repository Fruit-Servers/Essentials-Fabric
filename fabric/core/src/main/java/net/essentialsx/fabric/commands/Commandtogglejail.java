package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.config.Settings;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Commandtogglejail extends EssentialsCommand {
    public Commandtogglejail() {
        super("togglejail");
    }

    private long playtimeTicks(final MinecraftServer server, final User player) {
        if (player.isOnline()) {
            return player.getBase().getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        }
        return Commandplaytime.offlinePlaytimeTicks(server, player.getUUID());
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final User player = getPlayer(server, args, 0, true, true);
        final String senderName = sender.getName();
        mainCommand:
        if (!player.isJailed()) {
            if (!player.isOnline()) {
                if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.togglejail.offline")) {
                    sender.sendTl("mayNotJailOffline");
                    return;
                }
            }
            if (player.isAuthorized("essentials.jail.exempt")) {
                sender.sendTl("mayNotJail");
                return;
            }
            final String jailName;
            if (args.length > 1) {
                jailName = args[1];
            } else if (ess.getJails().getCount() == 1) {
                jailName = ess.getJails().getList().iterator().next();
            } else {
                break mainCommand;
            }
            // Check if jail exists
            ess.getJails().getJail(jailName);
            long displayTime = 0;
            long preTimeDiff = 0;
            if (args.length > 2) {
                final String time = getFinalArg(args, 2);
                displayTime = DateUtil.parseDateDiff(time, true);
                preTimeDiff = DateUtil.parseDateDiff(time, true, ess.getSettings().isJailOnlineTime());
            }
            final long timeDiff = preTimeDiff;
            final long finalDisplayTime = displayTime;
            final CompletableFuture<Boolean> future = getNewExceptionFuture(sender, commandLabel);
            future.thenAccept(success -> {
                if (success) {
                    player.setJailed(true);
                    player.sendTl("userJailed");
                    player.setJail(null);
                    player.setJail(jailName);
                    if (args.length > 2) {
                        player.setJailTimeout(timeDiff);
                        // 50 MSPT (milliseconds per tick)
                        player.setOnlineJailedTime(ess.getSettings().isJailOnlineTime() ? (playtimeTicks(server, player) + (timeDiff / 50)) : 0);
                    }
                    final String tlKey;
                    final Object[] objects;
                    if (timeDiff > 0) {
                        tlKey = "jailNotifyJailedFor";
                        objects = new Object[] {player.getName(), DateUtil.formatDateDiff(finalDisplayTime), senderName};
                        sender.sendTl("playerJailedFor", player.getName(), DateUtil.formatDateDiff(finalDisplayTime));
                    } else {
                        tlKey = "jailNotifyJailed";
                        objects = new Object[] {player.getName(), senderName, senderName};
                        sender.sendTl("playerJailed", player.getName());
                    }
                    ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral(tlKey, objects))));
                    ess.broadcastTl(null, u -> !u.isAuthorized("essentials.jail.notify"), tlKey, objects);
                }
            });
            if (player.isOnline()) {
                ess.getJails().sendToJail(player, jailName, future);
            } else {
                future.complete(true);
            }
            return;
        }
        if (args.length >= 2 && player.isJailed() && !args[1].equalsIgnoreCase(player.getJail())) {
            sender.sendTl("jailAlreadyIncarcerated", player.getJail());
            return;
        }
        if (args.length >= 2 && player.isJailed() && args[1].equalsIgnoreCase(player.getJail())) {
            final String unparsedTime = getFinalArg(args, 2);
            final long displayTimeDiff = DateUtil.parseDateDiff(unparsedTime, true);
            final long timeDiff = DateUtil.parseDateDiff(unparsedTime, true, ess.getSettings().isJailOnlineTime());
            player.setJailTimeout(timeDiff);
            player.setOnlineJailedTime(ess.getSettings().isJailOnlineTime() ? (playtimeTicks(server, player) + (timeDiff / 50)) : 0);
            sender.sendTl("jailSentenceExtended", DateUtil.formatDateDiff(displayTimeDiff));
            final String tlKey = "jailNotifySentenceExtended";
            final Object[] objects = new Object[] {player.getName(), DateUtil.formatDateDiff(displayTimeDiff), senderName};
            ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral(tlKey, objects))));
            ess.broadcastTl(null, u -> !u.isAuthorized("essentials.jail.notify"), tlKey, objects);
            return;
        }
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase(player.getJail()))) {
            if (!player.isJailed()) {
                throw new NotEnoughArgumentsException();
            }
            player.setJailed(false);
            player.setJailTimeout(0);
            player.sendTl("jailReleasedPlayerNotify");
            player.setJail(null);
            if (player.isOnline()) {
                final CompletableFuture<Boolean> future = getNewExceptionFuture(sender, commandLabel);
                future.thenAccept(success -> {
                    if (success) {
                        sender.sendTl("jailReleased", player.getName());
                    }
                });
                if (ess.getSettings().getTeleportWhenFreePolicy() == Settings.TeleportWhenFreePolicy.BACK) {
                    player.getAsyncTeleport().back(future);
                    future.exceptionally(e -> {
                        player.getAsyncTeleport().respawn(null, TeleportCause.PLUGIN, new CompletableFuture<>());
                        sender.sendTl("jailReleased", player.getName());
                        return false;
                    });
                } else if (ess.getSettings().getTeleportWhenFreePolicy() == Settings.TeleportWhenFreePolicy.SPAWN) {
                    player.getAsyncTeleport().respawn(null, TeleportCause.PLUGIN, future);
                }
                return;
            }
            sender.sendTl("jailReleased", player.getName());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            try {
                return new ArrayList<>(ess.getJails().getList());
            } catch (final Exception e) {
                return Collections.emptyList();
            }
        } else {
            return COMMON_DATE_DIFFS;
        }
    }
}
