package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.EssentialsFabric;
import net.essentialsx.fabric.listener.BlockListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unlimited items (11.4): restore consumed stacks after a use / place. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Unique
    private ItemStack essentials$beforeUse = ItemStack.EMPTY;
    @Unique
    private ItemStack essentials$beforeUseOn = ItemStack.EMPTY;

    @Inject(method = "useItem", at = @At("HEAD"))
    private void essentials$beforeUseItem(final ServerPlayer player, final Level level, final ItemStack stack, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir) {
        essentials$beforeUse = stack.copy();
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void essentials$afterUseItem(final ServerPlayer player, final Level level, final ItemStack stack, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir) {
        final BlockListener listener = EssentialsFabric.blocks();
        final ItemStack before = essentials$beforeUse;
        essentials$beforeUse = ItemStack.EMPTY;
        if (listener != null && cir.getReturnValue() != null && cir.getReturnValue().consumesAction()) {
            listener.afterItemUse(player, hand, before);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void essentials$beforeUseItemOn(final ServerPlayer player, final Level level, final ItemStack stack, final InteractionHand hand, final BlockHitResult hit, final CallbackInfoReturnable<InteractionResult> cir) {
        essentials$beforeUseOn = stack.copy();
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void essentials$afterUseItemOn(final ServerPlayer player, final Level level, final ItemStack stack, final InteractionHand hand, final BlockHitResult hit, final CallbackInfoReturnable<InteractionResult> cir) {
        final BlockListener listener = EssentialsFabric.blocks();
        final ItemStack before = essentials$beforeUseOn;
        essentials$beforeUseOn = ItemStack.EMPTY;
        if (listener != null && cir.getReturnValue() != null && cir.getReturnValue().consumesAction()) {
            listener.afterItemUse(player, hand, before);
        }
    }
}
