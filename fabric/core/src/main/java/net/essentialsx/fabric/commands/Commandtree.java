package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.Trees;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandtree extends EssentialsCommand {
    public Commandtree() {
        super("tree");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ResourceKey<ConfiguredFeature<?, ?>> tree = args.length > 0 ? Trees.small(args[0]) : null;
        if (tree == null) {
            throw new NotEnoughArgumentsException();
        }
        final LazyLocation target = LocationUtil.getTarget(user.getBase(), ess.getSettings().getMaxTreeCommandRange()).add(0, 1, 0);
        final BlockPos pos = target.blockPos();
        if (user.getWorld().getBlockState(pos).isSolid()) {
            throw new TranslatableException("treeFailure");
        }
        final boolean success = Trees.generate(user.getWorld(), pos, tree);
        if (success) {
            user.sendTl("treeSpawned");
        } else {
            throw new TranslatableException("treeFailure");
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("tree", "birch", "redwood", "redmushroom", "brownmushroom", "jungle", "junglebush", "swamp", "acacia", "darkoak", "cherry", "mangrove", "azalea"));
        } else {
            return Collections.emptyList();
        }
    }
}
