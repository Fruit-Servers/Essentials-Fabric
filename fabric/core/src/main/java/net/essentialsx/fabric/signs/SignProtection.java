package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Legacy protection sign (deprecated upstream, retained for parity).
 */
public class SignProtection extends EssentialsSign {
    private final transient Set<Block> protectedBlocks = Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.FURNACE, Blocks.DISPENSER);

    public SignProtection() {
        super("Protection");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        sign.setLine(3, "§4" + username);
        if (hasAdjacentBlock(sign.getLevel(), sign.getPos())) {
            final SignProtectionState state = isBlockProtected(sign.getLevel(), sign.getPos(), player, username, true);
            if (state == SignProtectionState.NOSIGN || state == SignProtectionState.OWNER || player.isAuthorized("essentials.signs.protection.override")) {
                sign.setLine(3, "§1" + username);
                return true;
            }
        }
        player.sendTl("signProtectInvalidLocation");
        return false;
    }

    @Override
    protected boolean onSignBreak(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        final SignProtectionState state = checkProtectionSign(sign, player, username);
        return state == SignProtectionState.OWNER;
    }

    public boolean hasAdjacentBlock(final ServerLevel level, final BlockPos pos, final BlockPos... ignoredBlocks) {
        for (final BlockPos b : getAdjacentBlocks(pos)) {
            if (protectedBlocks.contains(level.getBlockState(b).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private void checkIfSignsAreBroken(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws MaxMoneyException {
        final Map<BlockPos, SignProtectionState> signs = getConnectedSigns(level, pos, player, username, false);
        for (final Map.Entry<BlockPos, SignProtectionState> entry : signs.entrySet()) {
            if (entry.getValue() != SignProtectionState.NOSIGN) {
                final BlockPos sign = entry.getKey();
                if (!hasAdjacentBlock(level, sign, pos)) {
                    final BlockState state = level.getBlockState(sign);
                    level.setBlock(sign, Blocks.AIR.defaultBlockState(), 3);
                    try {
                        final Trade trade = new Trade(new ItemStack(state.getBlock().asItem(), 1), ess);
                        trade.pay(player, Trade.OverflowType.DROP);
                    } catch (final Exception ignored) {
                    }
                }
            }
        }
    }

    private Map<BlockPos, SignProtectionState> getConnectedSigns(final ServerLevel level, final BlockPos pos, final User user, final String username, final boolean secure) {
        final Map<BlockPos, SignProtectionState> signs = new HashMap<>();
        getConnectedSigns(level, pos, signs, user, username, secure ? 4 : 2);
        return signs;
    }

    private void getConnectedSigns(final ServerLevel level, final BlockPos pos, final Map<BlockPos, SignProtectionState> signs, final User user, final String username, final int depth) {
        for (final BlockPos b : getAdjacentBlocks(pos)) {
            if (signs.containsKey(b)) {
                continue;
            }
            final SignProtectionState check = checkProtectionSign(level, b, user, username);
            signs.put(b, check);
            if (protectedBlocks.contains(level.getBlockState(b).getBlock()) && depth > 0) {
                getConnectedSigns(level, b, signs, user, username, depth - 1);
            }
        }
    }

    private SignProtectionState checkProtectionSign(final ServerLevel level, final BlockPos pos, final User user, final String username) {
        if (MaterialUtil.isSign(level.getBlockState(pos))) {
            final BlockSign sign = new BlockSign(level, pos);
            if (sign.getLine(0).equals(this.getSuccessName())) {
                return checkProtectionSign(sign, user, username);
            }
        }
        return SignProtectionState.NOSIGN;
    }

    private SignProtectionState checkProtectionSign(final ISign sign, final User user, final String username) {
        if (user == null || username == null) {
            return SignProtectionState.NOT_ALLOWED;
        }
        if (user.isAuthorized("essentials.signs.protection.override")) {
            return SignProtectionState.OWNER;
        }
        if (isOwner(user.getEssentials(), user, sign, 3, "§1")) {
            return SignProtectionState.OWNER;
        }
        for (int i = 1; i <= 2; i++) {
            final String line = sign.getLine(i);
            if (line.startsWith("(") && line.endsWith(")") && user.inGroup(line.substring(1, line.length() - 1))) {
                return SignProtectionState.ALLOWED;
            } else if (line.equalsIgnoreCase(username)) {
                return SignProtectionState.ALLOWED;
            }
        }
        return SignProtectionState.NOT_ALLOWED;
    }

    private BlockPos[] getAdjacentBlocks(final BlockPos pos) {
        return new BlockPos[] {pos.north(), pos.south(), pos.east(), pos.west(), pos.below(), pos.above()};
    }

    public SignProtectionState isBlockProtected(final ServerLevel level, final BlockPos pos, final User user, final String username, final boolean secure) {
        final Map<BlockPos, SignProtectionState> signs = getConnectedSigns(level, pos, user, username, secure);
        SignProtectionState retstate = SignProtectionState.NOSIGN;
        for (final SignProtectionState state : signs.values()) {
            if (state == SignProtectionState.ALLOWED) {
                retstate = state;
            } else if (state == SignProtectionState.NOT_ALLOWED && retstate != SignProtectionState.ALLOWED) {
                retstate = state;
            }
        }
        if (!secure || retstate == SignProtectionState.NOSIGN) {
            for (final SignProtectionState state : signs.values()) {
                if (state == SignProtectionState.OWNER) {
                    return state;
                }
            }
        }
        return retstate;
    }

    @Override
    public Set<Block> getBlocks() {
        return protectedBlocks;
    }

    @Override
    public boolean areHeavyEventRequired() {
        return true;
    }

    private static String blockName(final ServerLevel level, final BlockPos pos) {
        return UserData.itemKey(level.getBlockState(pos).getBlock().asItem()).toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected boolean onBlockPlace(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException {
        for (final BlockPos adjBlock : getAdjacentBlocks(pos)) {
            final SignProtectionState state = isBlockProtected(level, adjBlock, player, username, true);
            if ((state == SignProtectionState.ALLOWED || state == SignProtectionState.NOT_ALLOWED) && !player.isAuthorized("essentials.signs.protection.override")) {
                player.sendTl("noPlacePermission", blockName(level, pos));
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean onBlockInteract(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException {
        final SignProtectionState state = isBlockProtected(level, pos, player, username, false);
        if (state == SignProtectionState.OWNER || state == SignProtectionState.NOSIGN || state == SignProtectionState.ALLOWED) {
            return true;
        }
        if (state == SignProtectionState.NOT_ALLOWED && player.isAuthorized("essentials.signs.protection.override")) {
            return true;
        }
        player.sendTl("noAccessPermission", blockName(level, pos));
        return false;
    }

    @Override
    protected boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException, MaxMoneyException {
        final SignProtectionState state = isBlockProtected(level, pos, player, username, false);
        if (state == SignProtectionState.OWNER || state == SignProtectionState.NOSIGN) {
            checkIfSignsAreBroken(level, pos, player, username, ess);
            return true;
        }
        if ((state == SignProtectionState.ALLOWED || state == SignProtectionState.NOT_ALLOWED) && player.isAuthorized("essentials.signs.protection.override")) {
            checkIfSignsAreBroken(level, pos, player, username, ess);
            return true;
        }
        player.sendTl("noDestroyPermission", blockName(level, pos));
        return false;
    }

    @Override
    public boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return isBlockProtected(level, pos, null, null, false) == SignProtectionState.NOSIGN;
    }

    @Override
    public boolean onBlockExplode(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return isBlockProtected(level, pos, null, null, false) == SignProtectionState.NOSIGN;
    }

    @Override
    public boolean onBlockBurn(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return isBlockProtected(level, pos, null, null, false) == SignProtectionState.NOSIGN;
    }

    @Override
    public boolean onBlockIgnite(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return isBlockProtected(level, pos, null, null, false) == SignProtectionState.NOSIGN;
    }

    @Override
    public boolean onBlockPush(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return isBlockProtected(level, pos, null, null, false) == SignProtectionState.NOSIGN;
    }

    public enum SignProtectionState {
        NOT_ALLOWED, ALLOWED, NOSIGN, OWNER
    }
}
