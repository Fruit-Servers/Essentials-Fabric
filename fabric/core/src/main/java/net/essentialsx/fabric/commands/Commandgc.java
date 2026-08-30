package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.management.ManagementFactory;

public class Commandgc extends EssentialsCommand {
    public Commandgc() {
        super("gc");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final double tps = ess.getTimer().getAverageTPS();
        final ChatColor color;
        if (tps >= 18.0) {
            color = ChatColor.GREEN;
        } else if (tps >= 15.0) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }
        sender.sendTl("uptime", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()));
        sender.sendTl("tps", "" + color + NumberUtil.formatDouble(tps));
        sender.sendTl("gcmax", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        sender.sendTl("gctotal", Runtime.getRuntime().totalMemory() / 1024 / 1024);
        sender.sendTl("gcfree", Runtime.getRuntime().freeMemory() / 1024 / 1024);
        for (final ServerLevel level : Worlds.all(server)) {
            String worldType = "World";
            if (Worlds.isNether(level)) {
                worldType = "Nether";
            } else if (Worlds.isEnd(level)) {
                worldType = "The End";
            }
            int entities = 0;
            for (final Entity ignored : level.getAllEntities()) {
                entities++;
            }
            final int chunks = level.getChunkSource().getLoadedChunksCount();
            // Ticking block entities are the closest server-side analogue to Bukkit's tile entity count.
            final int tileEntities = ((net.essentialsx.fabric.mixin.LevelAccessor) level).essentials$blockEntityTickers().size();
            sender.sendTl("gcWorld", worldType, Worlds.name(level), chunks, entities, tileEntities);
        }
    }
}
