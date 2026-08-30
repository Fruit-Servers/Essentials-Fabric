package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.signs.SignListener;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandeditsign extends EssentialsCommand {
    public Commandeditsign() {
        super("editsign");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || (args.length > 1 && !NumberUtil.isInt(args[1]))) {
            throw new NotEnoughArgumentsException();
        }
        final ModifiableSign sign = targetSign(user);
        if (sign == null) {
            throw new TranslatableException("editsignCommandTarget");
        }
        try {
            if (args[0].equalsIgnoreCase("set") && args.length > 2) {
                final String[] existingLines = sign.getLines();
                final int line = Integer.parseInt(args[1]) - 1;
                final String text = FormatUtil.formatString(user, "essentials.editsign", getFinalArg(args, 2)).trim();
                if (ChatColor.stripColor(text).length() > 15 && !user.isAuthorized("essentials.editsign.unlimited")) {
                    throw new TranslatableException("editsignCommandLimit");
                }
                existingLines[line] = text;
                if (applySignChange(sign, user, existingLines)) {
                    return;
                }
                user.sendTl("editsignCommandSetSuccess", line + 1, text);
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (args.length == 1) {
                    final String[] existingLines = sign.getLines();
                    for (int i = 0; i < 4; i++) {
                        existingLines[i] = "";
                    }
                    if (applySignChange(sign, user, existingLines)) {
                        return;
                    }
                    user.sendTl("editsignCommandClear");
                } else {
                    final String[] existingLines = sign.getLines();
                    final int line = Integer.parseInt(args[1]) - 1;
                    existingLines[line] = "";
                    if (applySignChange(sign, user, existingLines)) {
                        return;
                    }
                    user.sendTl("editsignCommandClearLine", line + 1);
                }
            } else if (args[0].equalsIgnoreCase("copy")) {
                final int line = args.length == 1 ? -1 : Integer.parseInt(args[1]) - 1;
                if (line == -1) {
                    for (int i = 0; i < 4; i++) {
                        // We use unformat here to prevent players from copying signs with colors that they do not have permission to use.
                        user.getSignCopy().set(i, FormatUtil.unformatString(user, "essentials.editsign", sign.getLine(i)));
                    }
                    user.sendTl("editsignCopy", commandLabel);
                } else {
                    user.getSignCopy().set(line, FormatUtil.unformatString(user, "essentials.editsign", sign.getLine(line)));
                    user.sendTl("editsignCopyLine", line + 1, commandLabel);
                }
            } else if (args[0].equalsIgnoreCase("paste")) {
                final int line = args.length == 1 ? -1 : Integer.parseInt(args[1]) - 1;
                final String[] existingLines = sign.getLines();
                if (line == -1) {
                    for (int i = 0; i < 4; i++) {
                        existingLines[i] = FormatUtil.formatString(user, "essentials.editsign", user.getSignCopy().get(i));
                    }
                    if (applySignChange(sign, user, existingLines)) {
                        return;
                    }
                    user.sendTl("editsignPaste", commandLabel);
                } else {
                    existingLines[line] = FormatUtil.formatString(user, "essentials.editsign", user.getSignCopy().get(line));
                    if (applySignChange(sign, user, existingLines)) {
                        return;
                    }
                    user.sendTl("editsignPasteLine", line + 1, commandLabel);
                }
            } else {
                throw new NotEnoughArgumentsException();
            }
        } catch (final IndexOutOfBoundsException e) {
            throw new TranslatableException(e, "editsignCommandNoLine");
        }
    }

    /**
     * Runs the Essentials sign pipeline (so editing a protected/special sign is validated like a
     * fresh sign placement) and writes the lines when allowed. Returns true when the edit was blocked.
     */
    private boolean applySignChange(final ModifiableSign sign, final User user, final String[] lines) {
        if (sign.isWaxed() && !user.isAuthorized("essentials.editsign.waxed.exempt")) {
            return true;
        }
        final SignListener listener = net.essentialsx.fabric.EssentialsFabric.signs();
        if (listener != null && !listener.onSignChange(sign.level(), sign.pos(), user.getBase(), lines)) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Sign change blocked for /editsign execution by " + user.getName());
            }
            return true;
        }
        for (int i = 0; i < 4; i++) {
            sign.setLine(i, lines[i]);
        }
        sign.update();
        return false;
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("set", "clear", "copy", "paste"));
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("1", "2", "3", "4"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && NumberUtil.isPositiveInt(args[1])) {
            final int line = Integer.parseInt(args[1]);
            final ModifiableSign sign = targetSign(user);
            if (sign != null && line <= 4) {
                return new ArrayList<>(List.of(FormatUtil.unformatString(user, "essentials.editsign", sign.getLine(line - 1))));
            }
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    private ModifiableSign targetSign(final User user) {
        final BlockPos target = user.getTargetBlock(5); //5 is a good number
        if (target == null) {
            return null;
        }
        final BlockEntity blockEntity = user.getWorld().getBlockEntity(target);
        if (!(blockEntity instanceof SignBlockEntity sign)) {
            return null;
        }
        return new ModifiableSign(sign, sign.isFacingFrontText(user.getBase()));
    }

    /** A sign side (front/back) with Bukkit-style legacy string line access. */
    private static final class ModifiableSign {
        private final SignBlockEntity sign;
        private final boolean front;

        ModifiableSign(final SignBlockEntity sign, final boolean front) {
            this.sign = sign;
            this.front = front;
        }

        String getLine(final int line) {
            if (line < 0 || line > 3) {
                throw new IndexOutOfBoundsException(line);
            }
            return Text.get().nativeToLegacy(sign.getText(front).getMessage(line, false));
        }

        String[] getLines() {
            final String[] lines = new String[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = getLine(i);
            }
            return lines;
        }

        void setLine(final int line, final String text) {
            if (line < 0 || line > 3) {
                throw new IndexOutOfBoundsException(line);
            }
            final Component component = Text.get().legacy(text == null ? "" : text);
            final SignText current = sign.getText(front);
            sign.setText(current.setMessage(line, component), front);
        }

        boolean isWaxed() {
            return sign.isWaxed();
        }

        net.minecraft.server.level.ServerLevel level() {
            return (net.minecraft.server.level.ServerLevel) sign.getLevel();
        }

        BlockPos pos() {
            return sign.getBlockPos();
        }

        void update() {
            sign.setChanged();
            if (sign.getLevel() != null) {
                sign.getLevel().sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
            }
        }
    }
}
