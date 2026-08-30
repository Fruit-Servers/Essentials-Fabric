package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Sign creation, use, break and protection hooks (Section 14). Invoked from Fabric callbacks
 * and from the sign-update mixin.
 */
public class SignListener {
    private final Essentials ess;

    public SignListener(final Essentials ess) {
        this.ess = ess;
    }

    /**
     * Called when a player submits sign text. {@code lines} are legacy-formatted and may be
     * edited in place. Returns false to cancel the edit.
     */
    public boolean onSignChange(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final String[] lines) {
        if (ess.getSettings().areSignsDisabled()) {
            return true;
        }
        final User user = ess.getUser(player);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = FormatUtil.formatString(user, "essentials.signs", lines[i] == null ? "" : lines[i]);
        }
        final String lColorlessTopLine = ChatColor.stripColor(lines[0]).toLowerCase().trim();
        if (!lColorlessTopLine.isEmpty()) {
            for (final Signs signs : Signs.values()) {
                final EssentialsSign sign = signs.getSign();
                final String successName = sign.getSuccessName(ess);
                if (successName == null) {
                    user.sendTl("errorWithMessage", "Please report this error to a staff member.");
                    return true;
                }
                final String lSuccessName = ChatColor.stripColor(successName.toLowerCase());
                if (lColorlessTopLine.contains(lSuccessName)) {
                    if (!ess.getSettings().enabledSigns().contains(sign) && ess.getSettings().getUnprotectedSignNames().contains(sign)) {
                        continue;
                    }
                    lines[0] = lColorlessTopLine;
                }
            }
        }
        final EssentialsSign.EditSign editSign = new EssentialsSign.EditSign(level, pos, lines);
        for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
            if (lines[0].equalsIgnoreCase(sign.getSuccessName(ess))) {
                return false;
            }
            if (lines[0].equalsIgnoreCase(sign.getTemplateName()) && !sign.onSignCreate(editSign, ess, player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Player right-clicks a block. Returns true when the interaction was consumed/cancelled.
     */
    public boolean onBlockInteract(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        if (ess.getSettings().areSignsDisabled()) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        if (MaterialUtil.isSign(state)) {
            final String csign = new EssentialsSign.BlockSign(level, pos).getLine(0);
            for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
                if (csign.equalsIgnoreCase(sign.getSuccessName(ess))) {
                    sign.onSignInteract(level, pos, player, ess);
                    return true;
                }
            }
        } else {
            for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
                if (sign.areHeavyEventRequired() && sign.getBlocks().contains(state.getBlock()) && !sign.onBlockInteract(level, pos, player, ess)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Player attempts to break a block. Returns true to cancel.
     */
    public boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        if (ess.getSettings().areSignsDisabled()) {
            return false;
        }
        try {
            return protectSignsAndBlocks(level, pos, player);
        } catch (final MaxMoneyException ex) {
            return true;
        }
    }

    public boolean protectSignsAndBlocks(final ServerLevel level, final BlockPos pos, final ServerPlayer player) throws MaxMoneyException {
        if (EssentialsSign.checkIfBlockBreaksSigns(ess, level, pos)) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Prevented that a block was broken next to a sign.");
            }
            return true;
        }
        final BlockState state = level.getBlockState(pos);
        if (MaterialUtil.isSign(state)) {
            final String line0 = new EssentialsSign.BlockSign(level, pos).getLine(0);
            for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
                if (line0.equalsIgnoreCase(sign.getSuccessName(ess)) && !sign.onSignBreak(level, pos, player, ess)) {
                    return true;
                }
            }
        }
        for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
            if (sign.areHeavyEventRequired() && sign.getBlocks().contains(state.getBlock()) && !sign.onBlockBreak(level, pos, player, ess)) {
                ess.getLogger().info("A block was protected by a sign.");
                return true;
            }
        }
        return false;
    }

    /**
     * Player places a block at {@code pos} against {@code against}. Returns true to cancel.
     */
    public boolean onBlockPlace(final ServerLevel level, final BlockPos pos, final BlockPos against, final ServerPlayer player, final BlockState placed) {
        if (ess.getSettings().areSignsDisabled()) {
            return false;
        }
        if (against != null) {
            final BlockState againstState = level.getBlockState(against);
            if (MaterialUtil.isSign(againstState) && EssentialsSign.isValidSign(ess, new EssentialsSign.BlockSign(level, against))) {
                return true;
            }
        }
        if (MaterialUtil.isSign(placed)) {
            return false;
        }
        for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
            if (sign.areHeavyEventRequired() && sign.getBlocks().contains(placed.getBlock()) && !sign.onBlockPlace(level, pos, player, ess)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Environmental destruction (explosion, fire, piston). Returns true to protect the block.
     */
    public boolean isProtectedFromEnvironment(final ServerLevel level, final BlockPos pos, final Cause cause) {
        if (ess.getSettings().areSignsDisabled()) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        if ((MaterialUtil.isSign(state) && EssentialsSign.isValidSign(ess, new EssentialsSign.BlockSign(level, pos))) || EssentialsSign.checkIfBlockBreaksSigns(ess, level, pos)) {
            return true;
        }
        for (final EssentialsSign sign : ess.getSettings().enabledSigns()) {
            if (sign.areHeavyEventRequired() && sign.getBlocks().contains(state.getBlock())) {
                final boolean allowed = switch (cause) {
                    case EXPLODE -> sign.onBlockExplode(level, pos, ess);
                    case BURN -> sign.onBlockBurn(level, pos, ess);
                    case IGNITE -> sign.onBlockIgnite(level, pos, ess);
                    case PUSH -> sign.onBlockPush(level, pos, ess);
                    case ENTITY -> sign.onBlockBreak(level, pos, ess);
                };
                if (!allowed) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isProtectedFromExplosion(final ServerLevel level, final List<BlockPos> positions) {
        for (final BlockPos pos : positions) {
            if (isProtectedFromEnvironment(level, pos, Cause.EXPLODE)) {
                return true;
            }
        }
        return false;
    }

    public enum Cause {
        EXPLODE, BURN, IGNITE, PUSH, ENTITY
    }
}
