package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Mob;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// This could be rewritten in a simpler form if we made a mapping of all Entity names to their types (which would also provide possible mod support)
public class Commandremove extends EssentialsCommand {
    public Commandremove() {
        super("remove");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        ServerLevel world = user.getWorld();
        int radius = 0;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (final NumberFormatException e) {
                world = Worlds.get(server, args[1]);
            }
        }
        if (args.length >= 3) {
            // This is to prevent breaking the old syntax
            radius = 0;
            world = Worlds.get(server, args[2]);
        }
        parseCommand(server, user.getSource(), args, world, radius);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final ServerLevel world = Worlds.get(server, args[1]);
        parseCommand(server, sender, args, world, 0);
    }

    private void parseCommand(final MinecraftServer server, final CommandSource sender, final String[] args, final ServerLevel world, final int radius) throws Exception {
        final List<String> types = new ArrayList<>();
        final List<String> customTypes = new ArrayList<>();
        if (world == null) {
            throw new TranslatableException("invalidWorld");
        }
        if (args[0].contentEquals("*") || args[0].contentEquals("all")) {
            types.add(0, "ALL");
        } else {
            for (final String entityType : args[0].split(",")) {
                ToRemove toRemove;
                try {
                    toRemove = ToRemove.valueOf(entityType.toUpperCase(Locale.ENGLISH));
                } catch (final Exception e) {
                    try {
                        toRemove = ToRemove.valueOf(entityType.concat("S").toUpperCase(Locale.ENGLISH));
                    } catch (final Exception ee) {
                        toRemove = ToRemove.CUSTOM;
                        customTypes.add(entityType);
                    }
                }
                types.add(toRemove.toString());
            }
        }
        removeHandler(sender, types, customTypes, world, radius);
    }

    private static boolean isMonster(final Entity e) {
        return e instanceof Enemy || e instanceof EnderDragon || e instanceof EnderDragonPart || e instanceof FlyingMob || e instanceof Slime;
    }

    private static boolean isPassive(final Entity e) {
        return e instanceof Animal || e instanceof Npc || e instanceof AbstractGolem || e instanceof WaterAnimal || e instanceof AmbientCreature;
    }

    private static boolean isTamedByPlayer(final Entity e) {
        return e instanceof TamableAnimal tamable && tamable.isTame() && ((OwnableEntity) tamable).getOwnerUUID() != null;
    }

    private void removeHandler(final CommandSource sender, final List<String> types, final List<String> customTypes, final ServerLevel world, int radius) {
        int removed = 0;
        if (radius > 0) {
            radius *= radius;
        }
        final ArrayList<ToRemove> removeTypes = new ArrayList<>();
        final ArrayList<Mob> customRemoveTypes = new ArrayList<>();
        for (final String s : types) {
            removeTypes.add(ToRemove.valueOf(s));
        }
        boolean warnUser = false;
        for (final String s : customTypes) {
            final Mob mobType = Mob.fromName(s);
            if (mobType == null) {
                warnUser = true;
            } else {
                customRemoveTypes.add(mobType);
            }
        }
        if (warnUser) {
            sender.sendTl("invalidMob");
        }
        final Vec3 origin = sender.isPlayer() ? sender.getPlayer().position() : null;
        final List<Entity> entities = new ArrayList<>();
        for (final Entity e : world.getAllEntities()) {
            entities.add(e);
        }
        for (final Entity e : entities) {
            if (radius > 0 && origin != null) {
                if (origin.distanceToSqr(e.position()) > radius) {
                    continue;
                }
            }
            if (e instanceof Player) {
                continue;
            }
            for (final ToRemove toRemove : removeTypes) {
                // We should skip any animals tamed by players unless we are specifically targeting them.
                if (isTamedByPlayer(e) && !removeTypes.contains(ToRemove.TAMED)) {
                    continue;
                }
                // We should skip any NAMED animals unless we are specifically targeting them.
                if (e instanceof LivingEntity && e.hasCustomName() && !removeTypes.contains(ToRemove.NAMED)) {
                    continue;
                }
                boolean remove = false;
                switch (toRemove) {
                    case TAMED:
                        remove = e instanceof TamableAnimal tamable && tamable.isTame();
                        break;
                    case NAMED:
                        remove = e instanceof LivingEntity && e.hasCustomName();
                        break;
                    case DROPS:
                        remove = e instanceof ItemEntity;
                        break;
                    case ARROWS:
                        remove = e instanceof Projectile;
                        break;
                    case BOATS:
                        remove = e instanceof Boat;
                        break;
                    case MINECARTS:
                        remove = e instanceof AbstractMinecart;
                        break;
                    case XP:
                        remove = e instanceof ExperienceOrb;
                        break;
                    case PAINTINGS:
                        remove = e instanceof Painting;
                        break;
                    case ITEMFRAMES:
                        remove = e instanceof ItemFrame;
                        break;
                    case ENDERCRYSTALS:
                        remove = e instanceof EndCrystal;
                        break;
                    case AMBIENT:
                        remove = e instanceof FlyingMob;
                        break;
                    case HOSTILE:
                    case MONSTERS:
                        remove = isMonster(e);
                        break;
                    case PASSIVE:
                    case ANIMALS:
                        remove = isPassive(e);
                        break;
                    case MOBS:
                        remove = isMonster(e) || isPassive(e);
                        break;
                    case ENTITIES:
                    case ALL:
                        remove = true;
                        break;
                    case CUSTOM:
                        for (final Mob type : customRemoveTypes) {
                            if (e.getType() == type.getType()) {
                                remove = true;
                                break;
                            }
                        }
                        break;
                }
                if (remove && !e.isRemoved()) {
                    e.discard();
                    removed++;
                }
            }
        }
        sender.sendTl("removed", removed);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>();
            for (final ToRemove toRemove : ToRemove.values()) {
                options.add(toRemove.name().toLowerCase(Locale.ENGLISH));
            }
            return options;
        } else if (args.length == 2) {
            return getWorlds(server);
        } else {
            return Collections.emptyList();
        }
    }

    private enum ToRemove {
        DROPS,
        ARROWS,
        BOATS,
        MINECARTS,
        XP,
        PAINTINGS,
        ITEMFRAMES,
        ENDERCRYSTALS,
        HOSTILE,
        MONSTERS,
        PASSIVE,
        ANIMALS,
        AMBIENT,
        MOBS,
        ENTITIES,
        ALL,
        CUSTOM,
        TAMED,
        NAMED
    }
}
