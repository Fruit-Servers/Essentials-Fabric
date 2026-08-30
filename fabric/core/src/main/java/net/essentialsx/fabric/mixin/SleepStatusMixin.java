package net.essentialsx.fabric.mixin;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.EssentialsFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/** Sleeping-ignored players (AFK / vanished / permission) do not count towards the night skip. */
@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true)
    private List<ServerPlayer> essentials$filterSleepers(final List<ServerPlayer> players) {
        final Essentials ess = EssentialsFabric.get();
        if (ess == null || ess.getSleepManager() == null) {
            return players;
        }
        final List<ServerPlayer> filtered = new ArrayList<>(players.size());
        for (final ServerPlayer player : players) {
            if (!ess.getSleepManager().isSleepingIgnored(player.getUUID())) {
                filtered.add(player);
            }
        }
        return filtered;
    }
}
