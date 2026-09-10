package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.Settings;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Entity/damage/death behaviour (Section 13.4): god mode, login attack delay, teleport
 * invulnerability, powertool PvP actions, keep-inventory/XP and back-on-death.
 */
public class EntityListener {
    private static final Pattern powertoolPlayer = Pattern.compile("\\{player\\}");
    private final Essentials ess;

    public EntityListener(final Essentials ess) {
        this.ess = ess;
    }

    public void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::allowDamage);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return onAttack(serverPlayer, entity);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> onCopyFrom(oldPlayer, newPlayer, alive));
    }

    // ------------------------------------------------------------ damage

    private boolean allowDamage(final LivingEntity entity, final DamageSource source, final float amount) {
        if (entity instanceof ServerPlayer defender) {
            final User defenderUser = ess.getUser(defender);
            if (defenderUser.isGodModeEnabled()) {
                defender.setRemainingFireTicks(0);
                defender.setAirSupply(defender.getMaxAirSupply());
                return false;
            }
            final Entity attackerEntity = source.getEntity();
            ServerPlayer attacker = null;
            if (attackerEntity instanceof ServerPlayer p) {
                attacker = p;
            } else if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer p) {
                attacker = p;
            }
            if (attacker != null) {
                final User attackerUser = ess.getUser(attacker);
                if (!onPlayerVsPlayerDamage(defender, attackerUser)) {
                    return false;
                }
                attackerUser.updateActivityOnInteract(true);
            }
        } else if (source.getEntity() instanceof ServerPlayer attackerPlayer) {
            final User attacker = ess.getUser(attackerPlayer);
            if (entity instanceof AgeableMob ageable && ess.getSettings().isMilkBucketEasterEggEnabled()) {
                final ItemStack hand = attackerPlayer.getMainHandItem();
                if (hand.is(Items.MILK_BUCKET)) {
                    ageable.setBaby(true);
                    attackerPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                    Inventories.update(attackerPlayer);
                    return false;
                }
            }
            attacker.updateActivityOnInteract(true);
        }
        return true;
    }

    private boolean onPlayerVsPlayerDamage(final ServerPlayer defender, final User attacker) {
        if (ess.getSettings().getLoginAttackDelay() > 0 && (System.currentTimeMillis() < (attacker.getLastLogin() + ess.getSettings().getLoginAttackDelay())) && !attacker.isAuthorized("essentials.pvpdelay.exempt")) {
            return false;
        }
        if (!defender.getUUID().equals(attacker.getUUID()) && (attacker.hasInvulnerabilityAfterTeleport() || ess.getUser(defender).hasInvulnerabilityAfterTeleport())) {
            return false;
        }
        if (attacker.isGodModeEnabled() && !attacker.isAuthorized("essentials.god.pvp")) {
            return false;
        }
        if (attacker.isHidden() && !attacker.isAuthorized("essentials.vanish.pvp")) {
            return false;
        }
        return true;
    }

    private InteractionResult onAttack(final ServerPlayer attackerPlayer, final Entity target) {
        final User attacker = ess.getUser(attackerPlayer);
        if (attacker.isJailed()) {
            if (target instanceof ServerPlayer) {
                return InteractionResult.FAIL;
            }
        }
        if (target instanceof ServerPlayer defender && attacker.arePowerToolsEnabled()) {
            final List<String> commandList = attacker.getPowertool(Inventories.getItemInHand(attackerPlayer));
            if (commandList != null && !commandList.isEmpty()) {
                for (final String tempCommand : commandList) {
                    final String command = powertoolPlayer.matcher(tempCommand).replaceAll(defender.getGameProfile().getName());
                    if (command != null && !command.isEmpty() && !command.equals(tempCommand)) {
                        ess.scheduleSyncDelayedTask(() -> {
                            ess.getCommandRegistry().dispatchAsUser(attacker, command);
                            ess.getLogger().info(String.format("[PT] %s issued server command: /%s", attacker.getName(), command));
                        });
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------ death (Section 13.4)

    /**
     * Called from {@code ServerPlayer#die} (HEAD) by {@code ServerPlayerMixin}, i.e. before vanilla drops
     * loot/XP in the same method. Hooked directly rather than through Fabric's ALLOW_DEATH redirect so that
     * another mod's mixin on {@code LivingEntity#hurt} cannot silently displace the keep-inv/keep-xp flags.
     */
    public void onDeath(final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (ess.getSettings().infoAfterDeath()) {
            final LazyLocation loc = user.getLocation();
            user.sendTl("infoAfterDeath", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
        }
        if (user.isAuthorized("essentials.back.ondeath") && !ess.getSettings().isCommandDisabled("back")) {
            user.setLastLocation();
            user.sendTl("backAfterDeath");
        }
        user.setKeepXpOnDeath(user.isAuthorized("essentials.keepxp"));
        user.setKeepInvOnDeath(user.isAuthorized("essentials.keepinv"));
        if (ess.getSettings().isDebug()) {
            ess.getLogger().info("Death of {}: keepxp={} keepinv={} keepInventory-rule={}", user.getName(), user.isKeepXpOnDeath(), user.isKeepInvOnDeath(), player.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY));
        }
        if (user.isKeepInvOnDeath()) {
            final Settings.KeepInvPolicy vanish = ess.getSettings().getVanishingItemsPolicy();
            final Settings.KeepInvPolicy bind = ess.getSettings().getBindingItemsPolicy();
            if (vanish != Settings.KeepInvPolicy.KEEP || bind != Settings.KeepInvPolicy.KEEP) {
                final List<ItemStack> removed = Inventories.removeItems(player, stack -> {
                    final ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    if (vanish != Settings.KeepInvPolicy.KEEP && hasEnchant(enchants, Enchantments.VANISHING_CURSE)) {
                        return true;
                    }
                    return bind != Settings.KeepInvPolicy.KEEP && hasEnchant(enchants, Enchantments.BINDING_CURSE);
                }, true);
                for (final ItemStack stack : removed) {
                    final ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    final boolean isVanish = hasEnchant(enchants, Enchantments.VANISHING_CURSE);
                    final Settings.KeepInvPolicy policy = isVanish ? vanish : bind;
                    if (policy == Settings.KeepInvPolicy.DROP) {
                        Inventories.dropNaturally(player, stack);
                    }
                }
            }
        }
    }

    private static boolean hasEnchant(final ItemEnchantments enchants, final ResourceKey<Enchantment> key) {
        for (final var holder : enchants.keySet()) {
            if (holder.is(key)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the death pipeline must skip the inventory drop for this player. */
    public boolean shouldKeepInventory(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        return user != null && user.isKeepInvOnDeath();
    }

    /** Whether the death pipeline must skip the XP drop for this player. */
    public boolean shouldKeepXp(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        return user != null && user.isKeepXpOnDeath();
    }

    private void onCopyFrom(final ServerPlayer oldPlayer, final ServerPlayer newPlayer, final boolean alive) {
        final User user = ess.getUsers().getOnlineUserCache().get(oldPlayer.getUUID());
        if (user == null) {
            return;
        }
        if (!alive) {
            final boolean keepInventoryRule = oldPlayer.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
            if (user.isKeepInvOnDeath() && !keepInventoryRule) {
                newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
            }
            if (user.isKeepXpOnDeath() && !keepInventoryRule) {
                newPlayer.experienceLevel = oldPlayer.experienceLevel;
                newPlayer.totalExperience = oldPlayer.totalExperience;
                newPlayer.experienceProgress = oldPlayer.experienceProgress;
            }
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Respawn of {}: restored keepxp={} (level {}) keepinv={}", user.getName(), user.isKeepXpOnDeath(), newPlayer.experienceLevel, user.isKeepInvOnDeath());
            }
            user.setKeepInvOnDeath(false);
            user.setKeepXpOnDeath(false);
        }
        user.update(newPlayer);
        ess.getUsers().getOnlineUserCache().put(newPlayer.getUUID(), user);
    }

    // ------------------------------------------------------------ misc hooks used by mixins

    /** Item pickup policy (AFK / vanish). */
    public boolean canPickup(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null) {
            return true;
        }
        return !((ess.getSettings().getDisableItemPickupWhileAfk() && user.isAfk()) || (user.isVanished() && !user.isAuthorizedCached("essentials.vanish.pickup")));
    }

    /** Mob targeting policy (vanished players are not targeted). */
    public boolean canBeTargeted(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        return user == null || !user.isVanished();
    }

    /** Natural regeneration policy for frozen AFK players. */
    public boolean allowNaturalRegen(final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        return user == null || !(user.isAfk() && ess.getSettings().getFreezeAfkPlayers());
    }

    public List<ServerPlayer> playersInRadius(final ServerPlayer center, final double radius) {
        final List<ServerPlayer> list = new ArrayList<>();
        for (final ServerPlayer other : center.serverLevel().players()) {
            if (other != center && other.distanceTo(center) <= radius) {
                list.add(other);
            }
        }
        return list;
    }
}
