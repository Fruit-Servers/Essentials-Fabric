package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandlightning extends EssentialsLoopCommand {
    public Commandlightning() {
        super("lightning");
    }

    /** Spawns a lightning bolt; {@code effectOnly} bolts do no damage/fire. */
    public static LightningBolt strike(final ServerLevel level, final Vec3 pos, final boolean effectOnly) {
        final LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return null;
        }
        bolt.moveTo(pos);
        bolt.setVisualOnly(effectOnly);
        level.addFreshEntity(bolt);
        return bolt;
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || !sender.isAuthorized("essentials.lightning.others")) {
            if (sender.isPlayer()) {
                final BlockPos target = sender.getUser().getTargetBlock(600);
                strike(sender.getPlayer().serverLevel(), Vec3.atBottomCenterOf(target), false);
                return;
            }
            throw new NotEnoughArgumentsException();
        }
        int power = 5;
        if (args.length > 1) {
            try {
                power = Integer.parseInt(args[1]);
            } catch (final NumberFormatException ignored) {
            }
        }
        final int finalPower = power;
        loopOnlinePlayersConsumer(server, sender, false, true, args[0], player -> {
            sender.sendTl("lightningUse", player.getDisplayName());
            final ServerPlayer base = player.getBase();
            final LightningBolt bolt = strike(base.serverLevel(), base.position(), true);
            if (!player.isGodModeEnabled() && bolt != null) {
                base.hurt(base.damageSources().lightningBolt(), finalPower);
            }
            if (ess.getSettings().warnOnSmite()) {
                player.sendTl("lightningSmited");
            }
        });
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User matchUser, final String[] args) {
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (!user.isAuthorized("essentials.lightning.others")) {
            return Collections.emptyList();
        } else {
            return super.getTabCompleteOptions(server, user, commandLabel, args);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("5"));
        } else {
            return Collections.emptyList();
        }
    }
}
