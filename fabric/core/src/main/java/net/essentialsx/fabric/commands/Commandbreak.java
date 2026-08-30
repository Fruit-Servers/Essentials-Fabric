package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Commandbreak extends EssentialsCommand {
    public Commandbreak() {
        super("break");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final BlockPos block = user.getTargetBlock(20);
        if (block == null) {
            throw new NoChargeException();
        }
        final BlockState state = user.getWorld().getBlockState(block);
        if (state.isAir()) {
            throw new NoChargeException();
        }
        if (state.is(Blocks.BEDROCK) && !user.isAuthorized("essentials.break.bedrock")) {
            throw new TranslatableException("noBreakBedrock");
        }
        if (net.essentialsx.fabric.EssentialsFabric.signs().onBlockBreak(user.getWorld(), block, user.getBase())) {
            throw new NoChargeException();
        }
        user.getWorld().removeBlock(block, false);
    }
}
