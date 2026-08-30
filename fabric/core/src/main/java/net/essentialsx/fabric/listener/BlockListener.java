package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.user.User;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Block/item interaction behaviour: signs, powertools, jail restrictions, unlimited items,
 * AFK activity, bed spawn updates and fly-click jump.
 */
public class BlockListener {
    private final Essentials ess;

    public BlockListener(final Essentials ess) {
        this.ess = ess;
    }

    public void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return true;
            }
            return onBlockBreak(level, pos, serverPlayer);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            return onLeftClickBlock(level, pos, serverPlayer);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            return onRightClickBlock(level, hitResult.getBlockPos(), serverPlayer, hand);
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            return onUseItem(serverPlayer, hand);
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            final User user = ess.getUser(serverPlayer);
            if (user.isJailed() && !user.isAuthorized("essentials.jail.allow-interact")) {
                return InteractionResult.FAIL;
            }
            user.updateActivityOnInteract(true);
            return InteractionResult.PASS;
        });
    }

    private boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (user.isJailed() && !user.isAuthorized("essentials.jail.allow-break")) {
            return false;
        }
        if (EssentialsFabric.signs().onBlockBreak(level, pos, player)) {
            return false;
        }
        user.updateActivityOnInteract(true);
        return true;
    }

    private InteractionResult onLeftClickBlock(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (user.isJailed() && !user.isAuthorized("essentials.jail.allow-block-damage")) {
            return InteractionResult.FAIL;
        }
        final ItemStack item = player.getMainHandItem();
        if (!item.isEmpty() && user.hasPowerTools() && user.arePowerToolsEnabled() && usePowertools(user, item)) {
            return InteractionResult.FAIL;
        }
        user.updateActivityOnInteract(true);
        return InteractionResult.PASS;
    }

    /** Left click in the air (from the swing packet mixin). */
    public void onLeftClickAir(final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (player.getAbilities().flying && user.isFlyClickJump()) {
            useFlyClickJump(user);
            return;
        }
        final ItemStack item = player.getMainHandItem();
        if (!item.isEmpty() && user.hasPowerTools() && user.arePowerToolsEnabled()) {
            usePowertools(user, item);
        }
        user.updateActivityOnInteract(true);
    }

    private InteractionResult onRightClickBlock(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final InteractionHand hand) {
        final User user = ess.getUser(player);
        if (user.isJailed() && !user.isAuthorized("essentials.jail.allow-interact")) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            if (EssentialsFabric.signs().onBlockInteract(level, pos, player)) {
                return InteractionResult.FAIL;
            }
            final BlockState state = level.getBlockState(pos);
            if (MaterialUtil.isBed(state) && ess.getSettings().getUpdateBedAtDaytime()) {
                if (!state.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED) && user.isAuthorized("essentials.sethome.bed") && net.essentialsx.fabric.utils.Worlds.isOverworldLike(level)) {
                    player.setRespawnPosition(level.dimension(), pos, player.getYRot(), false, false);
                }
            }
        }
        user.updateActivityOnInteract(true);
        return InteractionResult.PASS;
    }

    private InteractionResultHolder<ItemStack> onUseItem(final ServerPlayer player, final InteractionHand hand) {
        final User user = ess.getUser(player);
        final ItemStack stack = player.getItemInHand(hand);
        if (user.isJailed() && !user.isAuthorized("essentials.jail.allow-interact")) {
            return InteractionResultHolder.fail(stack);
        }
        user.updateActivityOnInteract(true);
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Unlimited items (Section 11.4): restore a consumed stack after a successful use/place.
     * Called from the game-mode mixin after the vanilla action returns.
     */
    public void afterItemUse(final ServerPlayer player, final InteractionHand hand, final ItemStack before) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null || before.isEmpty() || player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            return;
        }
        if (!user.hasUnlimited(before)) {
            return;
        }
        final ItemStack after = player.getItemInHand(hand);
        if (after.isEmpty() || after.getCount() < before.getCount() || (before.is(Items.WATER_BUCKET) || before.is(Items.LAVA_BUCKET) || before.is(Items.POWDER_SNOW_BUCKET) || before.is(Items.MILK_BUCKET)) && after.is(Items.BUCKET)) {
            player.setItemInHand(hand, before.copy());
            Inventories.update(player);
        }
    }

    private void useFlyClickJump(final User user) {
        try {
            final net.essentialsx.fabric.user.LazyLocation otarget = net.essentialsx.fabric.utils.LocationUtil.getTarget(user.getBase());
            ess.scheduleSyncDelayedTask(() -> {
                final ServerPlayer base = user.getBase();
                if (base == null) {
                    return;
                }
                net.essentialsx.fabric.user.LazyLocation loc = user.getLocation().withPosition(otarget.x(), user.getLocation().y(), otarget.z());
                final ServerLevel level = loc.level(ess.getServer());
                while (level != null && net.essentialsx.fabric.utils.LocationUtil.isBlockDamaging(level, loc.blockX(), loc.blockY() - 1, loc.blockZ())) {
                    loc = loc.add(0, 1, 0);
                }
                user.getAsyncTeleport().nowUnsafe(loc, net.essentialsx.fabric.teleport.TeleportCause.PLUGIN, new java.util.concurrent.CompletableFuture<>());
            });
        } catch (final Exception ex) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().warn(ex.getMessage(), ex);
            }
        }
    }

    private boolean usePowertools(final User user, final ItemStack item) {
        final List<String> commandList = user.getPowertool(item);
        if (commandList == null || commandList.isEmpty()) {
            return false;
        }
        boolean used = false;
        for (final String command : commandList) {
            if (command.contains("{player}")) {
                continue;
            } else if (command.startsWith("c:")) {
                used = true;
                final String chat = command.substring(2);
                ess.scheduleSyncDelayedTask(() -> PlayerChat.send(ess, user.getBase(), chat));
            } else {
                used = true;
                ess.scheduleSyncDelayedTask(() -> {
                    ess.getCommandRegistry().dispatchAsUser(user, command);
                    ess.getLogger().info(String.format("[PT] %s issued server command: /%s", user.getName(), command));
                });
            }
        }
        return used;
    }
}
