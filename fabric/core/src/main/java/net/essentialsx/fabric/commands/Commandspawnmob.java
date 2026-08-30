package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Mob;
import net.essentialsx.fabric.items.SpawnMob;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandspawnmob extends EssentialsCommand {
    public Commandspawnmob() {
        super("spawnmob");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException(user.playerTl("mobsAvailable", StringUtil.joinList(Mob.getMobList().toArray())));
        }
        final List<String> mobParts = SpawnMob.mobParts(args[0]);
        final List<String> mobData = SpawnMob.mobData(args[0]);
        int mobCount = 1;
        if (args.length >= 2) {
            mobCount = Integer.parseInt(args[1]);
        }
        if (mobParts.size() > 1 && !user.isAuthorized("essentials.spawnmob.stack")) {
            throw new TranslatableException("cannotStackMob");
        }
        if (args.length >= 3) {
            SpawnMob.spawnmob(ess, user.getSource(), getPlayer(server, user, args, 2), mobParts, mobData, mobCount);
            return;
        }
        SpawnMob.spawnmob(ess, user, mobParts, mobData, mobCount);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 3) {
            throw new NotEnoughArgumentsException(sender.tl("mobsAvailable", StringUtil.joinList(Mob.getMobList().toArray())));
        }
        final List<String> mobParts = SpawnMob.mobParts(args[0]);
        final List<String> mobData = SpawnMob.mobData(args[0]);
        SpawnMob.spawnmob(ess, sender, getPlayer(server, args, 2, true, false), mobParts, mobData, Integer.parseInt(args[1]));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Mob.getMobList());
        } else {
            return Collections.emptyList();
        }
    }
}
