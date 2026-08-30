package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Base class for Essentials signs (Section 14). Sign text is read from and written to the
 * {@link SignBlockEntity} front text; the owner UUID is stored in the block entity's
 * persistent Essentials tag.
 */
public class EssentialsSign {
    private static final String SIGN_OWNER_KEY = "sign-owner";
    protected static final BigDecimal MINTRANSACTION = new BigDecimal("0.01");
    private static final Set<Block> EMPTY_SET = new HashSet<>();
    protected final transient String signName;

    public EssentialsSign(final String signName) {
        this.signName = signName;
    }

    /**
     * Whether breaking {@code pos} would break an Essentials sign attached to it.
     */
    public static boolean checkIfBlockBreaksSigns(final Essentials ess, final ServerLevel level, final BlockPos pos) {
        final BlockPos above = pos.above();
        final BlockState aboveState = level.getBlockState(above);
        if (aboveState.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock && isValidSign(ess, new BlockSign(level, above))) {
            return true;
        }
        final BlockState belowState = level.getBlockState(pos.below());
        if (belowState.getBlock() instanceof net.minecraft.world.level.block.CeilingHangingSignBlock && isValidSign(ess, new BlockSign(level, pos.below()))) {
            return true;
        }
        final Direction[] directions = new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (final Direction direction : directions) {
            final BlockPos signPos = pos.relative(direction);
            final BlockState signState = level.getBlockState(signPos);
            if (signState.getBlock() instanceof WallSignBlock || signState.getBlock() instanceof WallHangingSignBlock) {
                try {
                    if (signState.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction && isValidSign(ess, new BlockSign(level, signPos))) {
                        return true;
                    }
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        return false;
    }

    public static boolean isValidSign(final Essentials ess, final ISign sign) {
        if (!sign.getLine(0).matches("§1\\[.*]")) {
            return false;
        }
        final String signName = ChatColor.stripColor(sign.getLine(0)).replaceAll("[^a-zA-Z]", "");
        for (final EssentialsSign essSign : ess.getSettings().enabledSigns()) {
            if (essSign.getName().equalsIgnoreCase(signName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sign creation hook: {@code lines} are the legacy-formatted lines the player submitted;
     * returns false to cancel (sign becomes ordinary) and may edit lines in place.
     */
    public final boolean onSignCreate(final ISign sign, final Essentials ess, final ServerPlayer player) {
        final User user = ess.getUser(player);
        if (!(user.isAuthorized("essentials.signs." + signName.toLowerCase(Locale.ENGLISH) + ".create") || user.isAuthorized("essentials.signs.create." + signName.toLowerCase(Locale.ENGLISH)))) {
            return true;
        }
        sign.setLine(0, Text.get().miniToLegacy(tlLiteral("signFormatFail", this.signName)));
        try {
            final boolean ret = onSignCreate(sign, user, getUsername(user), ess);
            if (ret) {
                sign.setLine(0, getSuccessName(ess));
            }
            return ret;
        } catch (final ChargeException | SignException ex) {
            showError(ess, user.getSource(), ex, signName);
        }
        setOwnerData(ess, user, sign);
        return true;
    }

    public String getSuccessName(final Essentials ess) {
        final String successName = getSuccessName();
        if (successName == null) {
            ess.getLogger().error("signFormatSuccess message must use the {0} argument.");
        }
        return successName;
    }

    public String getSuccessName() {
        String successName = Text.get().miniToLegacy(tlLiteral("signFormatSuccess", this.signName));
        if (successName.isEmpty() || !successName.contains(this.signName)) {
            successName = null;
        }
        return successName;
    }

    public String getTemplateName() {
        return Text.get().miniToLegacy(tlLiteral("signFormatTemplate", this.signName));
    }

    public String getName() {
        return this.signName;
    }

    public String getUsername(final User user) {
        return user.getName().substring(0, Math.min(user.getName().length(), 13));
    }

    public void setOwner(final Essentials ess, final User user, final ISign signProvider, final int nameIndex, final String namePrefix) {
        setOwnerData(ess, user, signProvider);
        signProvider.setLine(nameIndex, namePrefix + getUsername(user));
    }

    public void setOwnerData(final Essentials ess, final User user, final ISign signProvider) {
        signProvider.setData(SIGN_OWNER_KEY, user.getUUID().toString());
    }

    public boolean isOwner(final Essentials ess, final User user, final ISign signProvider, final int nameIndex, final String namePrefix) {
        final String stored = signProvider.getData(SIGN_OWNER_KEY);
        if (stored == null) {
            final boolean isLegacyOwner = FormatUtil.stripFormat(signProvider.getLine(nameIndex)).equalsIgnoreCase(getUsername(user));
            if (isLegacyOwner) {
                signProvider.setData(SIGN_OWNER_KEY, user.getUUID().toString());
            }
            return isLegacyOwner;
        }
        if (user.getUUID().toString().equals(stored)) {
            signProvider.setLine(nameIndex, namePrefix + getUsername(user));
            return true;
        }
        return false;
    }

    public final boolean onSignInteract(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final Essentials ess) {
        final ISign sign = new BlockSign(level, pos);
        final User user = ess.getUser(player);
        if (user.checkSignThrottle()) {
            return false;
        }
        try {
            if (player.isDeadOrDying() || !(user.isAuthorized("essentials.signs." + signName.toLowerCase(Locale.ENGLISH) + ".use") || user.isAuthorized("essentials.signs.use." + signName.toLowerCase(Locale.ENGLISH)))) {
                return false;
            }
            return onSignInteract(sign, user, getUsername(user), ess);
        } catch (final Exception ex) {
            showError(ess, user.getSource(), ex, signName);
            return false;
        }
    }

    public final boolean onSignBreak(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final Essentials ess) throws MaxMoneyException {
        final ISign sign = new BlockSign(level, pos);
        final User user = ess.getUser(player);
        try {
            if (!(user.isAuthorized("essentials.signs." + signName.toLowerCase(Locale.ENGLISH) + ".break") || user.isAuthorized("essentials.signs.break." + signName.toLowerCase(Locale.ENGLISH)))) {
                return false;
            }
            return onSignBreak(sign, user, getUsername(user), ess);
        } catch (final SignException ex) {
            showError(ess, user.getSource(), ex, signName);
            return false;
        }
    }

    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        return true;
    }

    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException, MaxMoneyException {
        return true;
    }

    protected boolean onSignBreak(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, MaxMoneyException {
        return true;
    }

    public final boolean onBlockPlace(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final Essentials ess) {
        final User user = ess.getUser(player);
        try {
            return onBlockPlace(level, pos, user, getUsername(user), ess);
        } catch (final ChargeException | SignException ex) {
            showError(ess, user.getSource(), ex, signName);
        }
        return false;
    }

    public final boolean onBlockInteract(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final Essentials ess) {
        final User user = ess.getUser(player);
        try {
            return onBlockInteract(level, pos, user, getUsername(user), ess);
        } catch (final ChargeException | SignException ex) {
            showError(ess, user.getSource(), ex, signName);
        }
        return false;
    }

    public final boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final Essentials ess) throws MaxMoneyException {
        final User user = ess.getUser(player);
        try {
            return onBlockBreak(level, pos, user, getUsername(user), ess);
        } catch (final SignException ex) {
            showError(ess, user.getSource(), ex, signName);
        }
        return false;
    }

    public boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return true;
    }

    public boolean onBlockExplode(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return true;
    }

    public boolean onBlockBurn(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return true;
    }

    public boolean onBlockIgnite(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return true;
    }

    public boolean onBlockPush(final ServerLevel level, final BlockPos pos, final Essentials ess) {
        return true;
    }

    protected boolean onBlockPlace(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        return true;
    }

    protected boolean onBlockInteract(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        return true;
    }

    protected boolean onBlockBreak(final ServerLevel level, final BlockPos pos, final User player, final String username, final Essentials ess) throws SignException, MaxMoneyException {
        return true;
    }

    public Set<Block> getBlocks() {
        return EMPTY_SET;
    }

    public boolean areHeavyEventRequired() {
        return false;
    }

    private String getSignText(final ISign sign, final int lineNumber) {
        return sign.getLine(lineNumber).trim();
    }

    protected final void validateTrade(final ISign sign, final int index, final Essentials ess) throws SignException {
        final String line = getSignText(sign, index);
        if (line.isEmpty()) {
            return;
        }
        final Trade trade = getTrade(sign, index, 0, ess);
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            sign.setLine(index, NumberUtil.shortCurrency(money, ess));
        }
    }

    protected final void validateTrade(final ISign sign, final int amountIndex, final int itemIndex, final User player, final Essentials ess) throws SignException {
        final String itemType = getSignText(sign, itemIndex);
        if (itemType.equalsIgnoreCase("exp") || itemType.equalsIgnoreCase("xp")) {
            final int amount = getIntegerPositive(getSignText(sign, amountIndex));
            sign.setLine(amountIndex, Integer.toString(amount));
            sign.setLine(itemIndex, "exp");
            return;
        }
        final Trade trade = getTrade(sign, amountIndex, itemIndex, player, ess);
        final ItemStack item = trade.getItemStack();
        sign.setLine(amountIndex, Integer.toString(item.getCount()));
        sign.setLine(itemIndex, itemType);
    }

    protected final Trade getTrade(final ISign sign, final int amountIndex, final int itemIndex, final User player, final Essentials ess) throws SignException {
        return getTrade(sign, amountIndex, itemIndex, player, false, ess);
    }

    protected final Trade getTrade(final ISign sign, final int amountIndex, final int itemIndex, final User player, final boolean allowId, final Essentials ess) throws SignException {
        final String itemType = getSignText(sign, itemIndex);
        if (itemType.equalsIgnoreCase("exp") || itemType.equalsIgnoreCase("xp")) {
            final int amount = getIntegerPositive(getSignText(sign, amountIndex));
            return new Trade(amount, ess);
        }
        final ItemStack item = getItemStack(itemType, 1, allowId, ess);
        final int amount = Math.min(getIntegerPositive(getSignText(sign, amountIndex)), item.getMaxStackSize() * 36);
        if (item.isEmpty() || amount < 1) {
            throw new SignException("moreThanZero");
        }
        item.setCount(amount);
        return new Trade(item, ess);
    }

    protected final void validateInteger(final ISign sign, final int index) throws SignException {
        final String line = getSignText(sign, index);
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final int quantity = getIntegerPositive(line);
        sign.setLine(index, Integer.toString(quantity));
    }

    protected final int getIntegerPositive(final String line) throws SignException {
        final int quantity = getInteger(line);
        if (quantity < 1) {
            throw new SignException("moreThanZero");
        }
        return quantity;
    }

    protected final int getInteger(final String line) throws SignException {
        try {
            return Integer.parseInt(line);
        } catch (final NumberFormatException ex) {
            throw new SignException("invalidSign");
        }
    }

    protected final ItemStack getItemStack(final String itemName, final int quantity, final Essentials ess) throws SignException {
        return getItemStack(itemName, quantity, false, ess);
    }

    protected final ItemStack getItemStack(final String itemName, final int quantity, final boolean allowId, final Essentials ess) throws SignException {
        try {
            final ItemStack item = ess.getItemDb().get(itemName);
            item.setCount(quantity);
            return item;
        } catch (final Exception ex) {
            if (ex instanceof TranslatableException) {
                final TranslatableException te = (TranslatableException) ex;
                throw new SignException(ex, te.getTlKey(), te.getArgs());
            }
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
    }

    protected final ItemStack getItemMeta(final ItemStack item, final String meta, final Essentials ess) throws SignException {
        return this.getItemMeta(null, item, meta, ess);
    }

    protected final ItemStack getItemMeta(final CommandSource source, final ItemStack item, final String meta, final Essentials ess) throws SignException {
        ItemStack stack = item;
        try {
            if (!meta.isEmpty()) {
                final MetaItemStack metaStack = new MetaItemStack(stack);
                final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
                metaStack.addStringMeta(source, allowUnsafe, meta, ess);
                stack = metaStack.getItemStack();
            }
        } catch (final Exception ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        return stack;
    }

    protected final BigDecimal getMoney(final String line, final Essentials ess) throws SignException {
        final boolean isMoney = line.matches("^[^0-9-.]?[.0-9]+[^0-9-.]?$");
        return isMoney ? getBigDecimalPositive(line, ess) : null;
    }

    protected final BigDecimal getBigDecimalPositive(final String line, final Essentials ess) throws SignException {
        final BigDecimal quantity = getBigDecimal(line, ess);
        if (quantity.compareTo(MINTRANSACTION) < 0) {
            throw new SignException("moreThanZero");
        }
        return quantity;
    }

    protected final BigDecimal getBigDecimal(final String line, final Essentials ess) throws SignException {
        try {
            return new BigDecimal(NumberUtil.sanitizeCurrencyString(line, ess));
        } catch (final ArithmeticException | NumberFormatException ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
    }

    protected final Trade getTrade(final ISign sign, final int index, final Essentials ess) throws SignException {
        return getTrade(sign, index, 1, ess);
    }

    protected final Trade getTrade(final ISign sign, final int index, final int decrement, final Essentials ess) throws SignException {
        return getTrade(sign, index, decrement, false, ess);
    }

    protected final Trade getTrade(final ISign sign, final int index, final int decrement, final boolean allowId, final Essentials ess) throws SignException {
        final String line = getSignText(sign, index);
        if (line.isEmpty()) {
            return new Trade(signName.toLowerCase(Locale.ENGLISH) + "sign", ess);
        }
        final BigDecimal money = getMoney(line, ess);
        if (money == null) {
            final String[] split = line.split("[ :]+", 2);
            if (split.length != 2) {
                throw new SignException("invalidCharge");
            }
            final int quantity = getIntegerPositive(split[0]);
            final String item = split[1].toLowerCase(Locale.ENGLISH);
            if (item.equalsIgnoreCase("times")) {
                sign.setLine(index, (quantity - decrement) + " times");
                sign.updateSign();
                return new Trade(signName.toLowerCase(Locale.ENGLISH) + "sign", ess);
            } else if (item.equalsIgnoreCase("exp") || item.equalsIgnoreCase("xp")) {
                sign.setLine(index, quantity + " exp");
                return new Trade(quantity, ess);
            } else {
                final ItemStack stack = getItemStack(item, quantity, allowId, ess);
                sign.setLine(index, quantity + " " + item);
                return new Trade(stack, ess);
            }
        } else {
            return new Trade(money, ess);
        }
    }

    private void showError(final Essentials ess, final CommandSource sender, final Throwable exception, final String signName) {
        ess.showError(sender, exception, "\\ sign: " + signName);
    }

    public interface ISign {
        String getLine(int index);

        void setLine(int index, String text);

        ServerLevel getLevel();

        BlockPos getPos();

        LazyLocation getLocation();

        void updateSign();

        String getData(String key);

        void setData(String key, String value);
    }

    /**
     * Sign being edited: lines are held in memory until the edit is committed by the mixin.
     */
    public static class EditSign implements ISign {
        private final ServerLevel level;
        private final BlockPos pos;
        private final String[] lines;

        public EditSign(final ServerLevel level, final BlockPos pos, final String[] lines) {
            this.level = level;
            this.pos = pos;
            this.lines = lines;
        }

        @Override
        public String getLine(final int index) {
            return stripPua(lines[index] == null ? "" : lines[index]);
        }

        @Override
        public void setLine(final int index, final String text) {
            lines[index] = text == null ? "" : text;
        }

        public String[] getLines() {
            return lines;
        }

        @Override
        public ServerLevel getLevel() {
            return level;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public LazyLocation getLocation() {
            return LazyLocation.of(level, pos);
        }

        @Override
        public void updateSign() {
        }

        @Override
        public String getData(final String key) {
            return BlockSign.readData(level, pos, key);
        }

        @Override
        public void setData(final String key, final String value) {
            BlockSign.writeData(level, pos, key, value);
        }
    }

    public static class BlockSign implements ISign {
        private final ServerLevel level;
        private final BlockPos pos;

        public BlockSign(final ServerLevel level, final BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        private SignBlockEntity entity() {
            final BlockEntity be = level.getBlockEntity(pos);
            return be instanceof SignBlockEntity sign ? sign : null;
        }

        @Override
        public String getLine(final int index) {
            final SignBlockEntity sign = entity();
            if (sign == null) {
                return "";
            }
            final Component line = sign.getFrontText().getMessage(index, false);
            return stripPua(Text.get().nativeToLegacy(line));
        }

        @Override
        public void setLine(final int index, final String text) {
            final SignBlockEntity sign = entity();
            if (sign == null) {
                return;
            }
            final SignText front = sign.getFrontText();
            sign.setText(front.setMessage(index, Text.get().legacy(text == null ? "" : text)), true);
            updateSign();
        }

        @Override
        public ServerLevel getLevel() {
            return level;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public LazyLocation getLocation() {
            return LazyLocation.of(level, pos);
        }

        @Override
        public void updateSign() {
            final SignBlockEntity sign = entity();
            if (sign != null) {
                sign.setChanged();
                level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
            }
        }

        @Override
        public String getData(final String key) {
            return readData(level, pos, key);
        }

        @Override
        public void setData(final String key, final String value) {
            writeData(level, pos, key, value);
        }

        static String readData(final ServerLevel level, final BlockPos pos, final String key) {
            final BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) {
                return null;
            }
            final CompoundTag tag = ((EssentialsSignData) sign).essentials$getData();
            return tag != null && tag.contains(key) ? tag.getString(key) : null;
        }

        static void writeData(final ServerLevel level, final BlockPos pos, final String key, final String value) {
            final BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) {
                return;
            }
            final EssentialsSignData data = (EssentialsSignData) sign;
            CompoundTag tag = data.essentials$getData();
            if (tag == null) {
                tag = new CompoundTag();
            }
            tag.putString(key, value);
            data.essentials$setData(tag);
            sign.setChanged();
        }
    }

    /** Implemented by the SignBlockEntity mixin to persist Essentials sign data. */
    public interface EssentialsSignData {
        CompoundTag essentials$getData();

        void essentials$setData(CompoundTag tag);
    }

    static String stripPua(final String input) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : input.toCharArray()) {
            if (c < 0xF700 || c > 0xF747) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static boolean isSignBlock(final BlockState state) {
        return state.getBlock() instanceof SignBlock;
    }
}
