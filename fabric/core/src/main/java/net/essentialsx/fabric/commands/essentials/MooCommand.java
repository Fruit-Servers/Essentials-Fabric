package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MooCommand extends EssentialsTreeNode {
    private static final String[] CONSOLE_MOO = new String[] {"         (__)", "         (oo)", "   /------\\/", "  / |    ||", " *  /\\---/\\", "    ~~   ~~", "....\"Have you mooed today?\"..."};
    private static final String[] PLAYER_MOO = new String[] {"            (__)", "            (oo)", "   /------\\/", "  /  |      | |", " *  /\\---/\\", "    ~~    ~~", "....\"Have you mooed today?\"..."};

    public MooCommand() {
        super(new String[] {"moo"}, true);
    }

    private static void moo(final ServerPlayer player) {
        for (final String line : PLAYER_MOO) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line));
        }
        player.playNotifySound(SoundEvents.COW_MILK, SoundSource.PLAYERS, 1, 1.0f);
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && args[0].equals("moo")) {
            for (final String s : CONSOLE_MOO) {
                ess.getLogger().info(s);
            }
            for (final ServerPlayer player : ess.getOnlinePlayers()) {
                moo(player);
            }
        } else {
            if (sender.isPlayer()) {
                moo(sender.getPlayer());
            } else {
                for (final String s : CONSOLE_MOO) {
                    sender.sendMessage(s);
                }
            }
        }
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("moo"));
        }
        return Collections.emptyList();
    }
}
