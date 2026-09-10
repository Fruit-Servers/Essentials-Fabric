package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsToggleCommand;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.essentialsx.fabric.utils.TriState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Toggles a permanent night vision effect (Fabric-only addition, not an upstream command).
 * The effect is a vanilla infinite-duration status effect, so it survives relogs and death
 * only as vanilla would, and can be cleared with /nightvision off or vanilla /effect clear.
 */
public class Commandnightvision extends EssentialsToggleCommand {
    public Commandnightvision() {
        super("nightvision", "essentials.nightvision.others");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        toggleOtherPlayers(server, sender, args);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        handleToggleWithArgs(server, user, args);
    }

    /**
     * Toggling others requires essentials.nightvision.others to be granted by the permission
     * provider (directly or via a provider-side wildcard). Operator status and Essentials' own
     * fallbacks do not imply it, unlike the other toggle commands.
     */
    @Override
    protected boolean canToggleOthers(final User user) {
        return user.getBase() != null && ess.getPermissionsHandler().isPermissionSetExact(user.getBase(), "essentials.nightvision.others") == TriState.TRUE;
    }

    private static boolean hasPermanentNightVision(final ServerPlayer player) {
        final MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        return effect != null && effect.isInfiniteDuration();
    }

    @Override
    protected void togglePlayer(final CommandSource sender, final User user, Boolean enabled) {
        final ServerPlayer player = user.getBase();
        if (enabled == null) {
            enabled = !hasPermanentNightVision(player);
        }
        if (enabled) {
            // Replace any potion-based (finite) night vision with the permanent one; no particles, keep the HUD icon.
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        user.sendTl("nightVision", CommonPlaceholders.enableDisable(user.getSource(), enabled), user.getDisplayName());
        if (!sender.isPlayer() || !sender.getPlayer().equals(player)) {
            sender.sendTl("nightVision", CommonPlaceholders.enableDisable(user.getSource(), enabled), user.getDisplayName());
        }
    }
}
