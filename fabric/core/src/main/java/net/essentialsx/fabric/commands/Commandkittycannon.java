package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

// This command is not documented on the wiki #EasterEgg
public class Commandkittycannon extends EssentialsCommand {
    private static final Random RANDOM = new Random();

    public Commandkittycannon() {
        super("kittycannon");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ServerPlayer player = user.getBase();
        final ServerLevel level = player.serverLevel();
        final Cat cat = EntityType.CAT.create(level);
        if (cat == null) {
            return;
        }
        final Vec3 eye = player.getEyePosition();
        cat.moveTo(eye.x, eye.y, eye.z, player.getYRot(), player.getXRot());
        final List<Holder.Reference<CatVariant>> variants = BuiltInRegistries.CAT_VARIANT.holders().toList();
        if (!variants.isEmpty()) {
            cat.setVariant(variants.get(RANDOM.nextInt(variants.size())));
        }
        cat.setTame(true, true);
        cat.setOwnerUUID(player.getUUID());
        cat.setBaby(true);
        cat.setDeltaMovement(player.getLookAngle().scale(2));
        level.addFreshEntity(cat);
        ess.scheduleSyncDelayedTask(() -> {
            final Vec3 loc = cat.position();
            cat.discard();
            level.explode(null, loc.x, loc.y, loc.z, 0F, Level.ExplosionInteraction.NONE);
        }, 20);
    }
}
