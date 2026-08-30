package net.essentialsx.fabric.spawn;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandsetspawn extends EssentialsCommand {
    private final SpawnStorage spawns;

    public Commandsetspawn(final SpawnStorage spawns) {
        super("setspawn");
        this.spawns = spawns;
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        final String group = args.length > 0 ? getFinalArg(args, 0) : "default";
        spawns.setSpawn(user.getLocation(), group);
        user.sendTl("spawnSet", group);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        return Collections.emptyList();
    }
}
