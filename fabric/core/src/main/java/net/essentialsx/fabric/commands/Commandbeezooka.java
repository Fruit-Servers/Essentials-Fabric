package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Commandbeezooka extends EssentialsCommand {
    public Commandbeezooka() {
        super("beezooka");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ServerPlayer player = user.getBase();
        final ServerLevel level = player.serverLevel();
        final Bee bee = EntityType.BEE.create(level);
        if (bee == null) {
            return;
        }
        final Vec3 eye = player.getEyePosition();
        bee.moveTo(eye.x, eye.y, eye.z, player.getYRot(), player.getXRot());
        bee.setDeltaMovement(player.getLookAngle().scale(2));
        level.addFreshEntity(bee);
        ess.scheduleSyncDelayedTask(() -> {
            final Vec3 loc = bee.position();
            bee.discard();
            level.explode(null, loc.x, loc.y, loc.z, 0F, Level.ExplosionInteraction.NONE);
        }, 20);
    }
}
