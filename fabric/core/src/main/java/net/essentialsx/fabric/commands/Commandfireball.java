package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Commandfireball extends EssentialsCommand {
    /** Marker used by listeners to recognise Essentials-launched projectiles. */
    public static final String FIREBALL_TAG = "ess_fireball_proj";
    private static final Map<String, BiFunction<ServerPlayer, Vec3, Projectile>> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("fireball", (p, d) -> new LargeFireball(p.level(), p, d, 1));
        TYPES.put("small", (p, d) -> new SmallFireball(p.level(), p, d));
        TYPES.put("large", (p, d) -> new LargeFireball(p.level(), p, d, 1));
        TYPES.put("arrow", (p, d) -> new Arrow(p.level(), p, new ItemStack(Items.ARROW), null));
        TYPES.put("skull", (p, d) -> new WitherSkull(p.level(), p, d));
        TYPES.put("egg", (p, d) -> new ThrownEgg(p.level(), p));
        TYPES.put("snowball", (p, d) -> new Snowball(p.level(), p));
        TYPES.put("expbottle", (p, d) -> new ThrownExperienceBottle(p.level(), p));
        TYPES.put("dragon", (p, d) -> new DragonFireball(p.level(), p, d));
        TYPES.put("splashpotion", (p, d) -> {
            final ThrownPotion potion = new ThrownPotion(p.level(), p);
            final ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            potion.setItem(stack);
            return potion;
        });
        TYPES.put("lingeringpotion", (p, d) -> {
            final ThrownPotion potion = new ThrownPotion(p.level(), p);
            final ItemStack stack = new ItemStack(Items.LINGERING_POTION);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            potion.setItem(stack);
            return potion;
        });
        TYPES.put("trident", (p, d) -> new ThrownTrident(p.level(), p, new ItemStack(Items.TRIDENT)));
        TYPES.put("windcharge", (p, d) -> new WindCharge(p, p.level(), p.getX(), p.getEyeY(), p.getZ()));
    }

    public Commandfireball() {
        super("fireball");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final String type = args.length > 0 && TYPES.containsKey(args[0]) ? args[0] : "fireball";
        double speed = 2;
        final boolean ride = args.length > 2 && args[2].equalsIgnoreCase("ride") && user.isAuthorized("essentials.fireball.ride");
        if (args.length > 1) {
            try {
                speed = Double.parseDouble(args[1]);
                speed = Double.max(0, Double.min(speed, ess.getSettings().getMaxProjectileSpeed()));
            } catch (final Exception ignored) {
            }
        }
        if (!user.isAuthorized("essentials.fireball." + type)) {
            throw new TranslatableException("noPerm", "essentials.fireball." + type);
        }
        final ServerPlayer player = user.getBase();
        final ServerLevel level = player.serverLevel();
        final Vec3 direction = player.getLookAngle().scale(speed);
        final Vec3 eye = player.getEyePosition();
        final Projectile projectile = TYPES.get(type).apply(player, direction);
        projectile.setPos(eye.x + direction.x, eye.y + direction.y, eye.z + direction.z);
        projectile.setOwner(player);
        projectile.setDeltaMovement(direction);
        projectile.addTag(FIREBALL_TAG);
        level.addFreshEntity(projectile);
        if (ride) {
            player.startRiding(projectile, true);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return TYPES.keySet().stream()
                .filter(type -> user.isAuthorized("essentials.fireball." + type))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("1", "2", "3", "4", "5"));
        } else {
            return Collections.emptyList();
        }
    }
}
