package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commanditemlore extends EssentialsCommand {
    public Commanditemlore() {
        super("itemlore");
    }

    private static List<Component> lore(final ItemStack item) {
        return new ArrayList<>(item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
    }

    private static void setLore(final ItemStack item, final List<Component> lines) {
        if (lines.isEmpty()) {
            item.remove(DataComponents.LORE);
        } else {
            item.set(DataComponents.LORE, new ItemLore(lines));
        }
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack item = Inventories.getItemInHand(user.getBase());
        if (item == null || item.isEmpty()) {
            throw new TranslatableException("itemloreInvalidItem");
        }
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final List<Component> lore = lore(item);
        final int loreSize = lore.size();
        if (args[0].equalsIgnoreCase("add") && args.length > 1) {
            if (loreSize >= ess.getSettings().getMaxItemLore() && !user.isAuthorized("essentials.itemlore.bypass")) {
                throw new TranslatableException("itemloreMaxLore");
            }
            final String line = FormatUtil.formatString(user, "essentials.itemlore", getFinalArg(args, 1)).trim();
            lore.add(Text.get().legacy(line));
            setLore(item, lore);
            user.sendTl("itemloreSuccess", line);
        } else if (args[0].equalsIgnoreCase("clear")) {
            setLore(item, new ArrayList<>());
            user.sendTl("itemloreClear");
        } else if (args[0].equalsIgnoreCase("set") && args.length > 2 && NumberUtil.isInt(args[1])) {
            if (loreSize == 0) {
                throw new TranslatableException("itemloreNoLore");
            }
            final int line = Integer.parseInt(args[1]);
            final String newLine = FormatUtil.formatString(user, "essentials.itemlore", getFinalArg(args, 2)).trim();
            try {
                lore.set(line - 1, Text.get().legacy(newLine));
            } catch (final Exception e) {
                throw new TranslatableException(e, "itemloreNoLine", line);
            }
            setLore(item, lore);
            user.sendTl("itemloreSuccessLore", line, newLine);
        } else {
            throw new NotEnoughArgumentsException();
        }
        Inventories.update(user.getBase());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("add", "set", "clear"));
        } else if (args.length == 2) {
            if (args[0].toLowerCase(Locale.ENGLISH).equals("set")) {
                final ItemStack item = Inventories.getItemInHand(user.getBase());
                if (item != null && !item.isEmpty()) {
                    final List<String> lineNumbers = new ArrayList<>();
                    for (int i = 1; i <= lore(item).size(); i++) {
                        lineNumbers.add(String.valueOf(i));
                    }
                    return lineNumbers;
                }
            }
            return Collections.emptyList();
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set") && NumberUtil.isInt(args[1])) {
                final int i = Integer.parseInt(args[1]);
                final ItemStack item = Inventories.getItemInHand(user.getBase());
                if (item != null && !item.isEmpty() && lore(item).size() >= i && i > 0) {
                    return new ArrayList<>(List.of(FormatUtil.unformatString(user, "essentials.itemlore", Text.get().nativeToLegacy(lore(item).get(i - 1)))));
                }
            }
            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }
}
