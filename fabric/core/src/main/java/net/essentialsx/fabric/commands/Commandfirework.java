package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Port of upstream /firework:
 * <ul>
 *     <li>{@code /firework clear} - clears all effects on the held rocket</li>
 *     <li>{@code /firework power <int>} - sets flight duration</li>
 *     <li>{@code /firework fire [amount|direction]} - launches copies of the held rocket</li>
 *     <li>{@code /firework color:<c[,c]> [fade:<c>] [shape:<s>] [effect:<e>]} - adds an explosion</li>
 * </ul>
 */
public class Commandfirework extends EssentialsCommand {
    public Commandfirework() {
        super("firework");
    }

    private static Fireworks fireworks(final ItemStack stack) {
        return stack.getOrDefault(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final ItemStack stack = user.getItemInHand();
        if (stack == null || !MaterialUtil.isFirework(stack.getItem())) {
            throw new TranslatableException("holdFirework");
        }
        if (args[0].equalsIgnoreCase("clear")) {
            stack.set(DataComponents.FIREWORKS, new Fireworks(fireworks(stack).flightDuration(), List.of()));
            user.sendTl("fireworkEffectsCleared");
        } else if (args.length > 1 && (args[0].equalsIgnoreCase("power") || args[0].equalsIgnoreCase("p"))) {
            try {
                final int power = Integer.parseInt(args[1]);
                stack.set(DataComponents.FIREWORKS, new Fireworks(Math.max(0, power > 3 ? 4 : power), fireworks(stack).explosions()));
            } catch (final NumberFormatException e) {
                throw new TranslatableException("invalidFireworkFormat", args[1], args[0]);
            }
        } else if ((args[0].equalsIgnoreCase("fire") || args[0].equalsIgnoreCase("f")) && user.isAuthorized("essentials.firework.fire")) {
            int amount = 1;
            boolean direction = false;
            if (args.length > 1) {
                if (NumberUtil.isInt(args[1])) {
                    final int serverLimit = ess.getSettings().getSpawnMobLimit();
                    amount = Integer.parseInt(args[1]);
                    if (amount > serverLimit) {
                        amount = serverLimit;
                        user.sendTl("mobSpawnLimit");
                    }
                } else {
                    direction = true;
                }
            }
            final ServerPlayer player = user.getBase();
            for (int i = 0; i < amount; i++) {
                final ItemStack copy = stack.copyWithCount(1);
                final FireworkRocketEntity firework;
                if (direction) {
                    final Fireworks data = fireworks(copy);
                    if (data.flightDuration() > 1) {
                        copy.set(DataComponents.FIREWORKS, new Fireworks(1, data.explosions()));
                    }
                    firework = new FireworkRocketEntity(player.level(), copy, player.getX(), player.getY(), player.getZ(), true);
                    final Vec3 vector = player.getLookAngle().scale(0.070);
                    firework.setDeltaMovement(vector);
                } else {
                    firework = new FireworkRocketEntity(player.level(), player.getX(), player.getY(), player.getZ(), copy);
                }
                player.serverLevel().addFreshEntity(firework);
            }
        } else {
            final MetaItemStack mStack = new MetaItemStack(stack);
            for (final String arg : args) {
                try {
                    mStack.addFireworkMeta(user.getSource(), true, arg, ess);
                } catch (final Exception e) {
                    user.sendTl("fireworkSyntax");
                    throw e;
                }
            }
            if (mStack.isValidFirework()) {
                final Fireworks data = fireworks(stack);
                final FireworkExplosion effect = mStack.getFireworkBuilder().build();
                if (!data.explosions().isEmpty() && !user.isAuthorized("essentials.firework.multiple")) {
                    throw new TranslatableException("multipleCharges");
                }
                final List<FireworkExplosion> explosions = new ArrayList<>(data.explosions());
                explosions.add(effect);
                stack.set(DataComponents.FIREWORKS, new Fireworks(data.flightDuration(), explosions));
            } else {
                user.sendTl("fireworkSyntax");
                throw new TranslatableException("fireworkColor");
            }
        }
        Inventories.update(user.getBase());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>();
            if (args[0].startsWith("color:")) {
                final String prefix;
                if (args[0].contains(",")) {
                    prefix = args[0].substring(0, args[0].lastIndexOf(',') + 1);
                } else {
                    prefix = "color:";
                }
                for (final DyeColor color : DyeColor.values()) {
                    options.add(prefix + color.name().toLowerCase(Locale.ENGLISH) + ",");
                }
                return options;
            }
            options.add("clear");
            options.add("power");
            options.add("color:");
            if (user.isAuthorized("essentials.firework.fire")) {
                options.add("fire");
            }
            return options;
        } else if (args.length == 2) {
            if (args[0].equals("power")) {
                return new ArrayList<>(List.of("1", "2", "3", "4"));
            } else if (args[0].equals("fire")) {
                return new ArrayList<>(List.of("1"));
            } else if (args[0].startsWith("color:")) {
                final List<String> options = new ArrayList<>();
                if (!args[1].startsWith("fade:")) {
                    args[1] = "fade:";
                }
                final String prefix;
                if (args[1].contains(",")) {
                    prefix = args[1].substring(0, args[1].lastIndexOf(',') + 1);
                } else {
                    prefix = "fade:";
                }
                for (final DyeColor color : DyeColor.values()) {
                    options.add(prefix + color.name().toLowerCase(Locale.ENGLISH) + ",");
                }
                return options;
            } else {
                return Collections.emptyList();
            }
        } else if (args.length == 3 && args[0].startsWith("color:")) {
            return new ArrayList<>(List.of("shape:star", "shape:ball", "shape:large", "shape:creeper", "shape:burst"));
        } else if (args.length == 4 && args[0].startsWith("color:")) {
            return new ArrayList<>(List.of("effect:trail", "effect:twinkle", "effect:trail,twinkle", "effect:twinkle,trail"));
        } else {
            return Collections.emptyList();
        }
    }
}
