package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commanditemname extends EssentialsCommand {
    public static final String PERM_PREFIX = "essentials.itemname.prevent-type.";

    public Commanditemname() {
        super("itemname");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack item = Inventories.getItemInHand(user.getBase());
        if (item == null || item.isEmpty()) {
            user.sendTl("itemnameInvalidItem");
            return;
        }
        final TriState wildcard = user.isAuthorizedExact(PERM_PREFIX + "*");
        final TriState material = user.isAuthorizedExact(PERM_PREFIX + UserData.itemKey(item).toLowerCase(Locale.ENGLISH));
        if ((wildcard == TriState.TRUE && material != TriState.FALSE) || ((wildcard != TriState.TRUE) && material == TriState.TRUE)) {
            user.sendTl("itemnameInvalidItem");
            return;
        }
        String name = FormatUtil.formatString(user, "essentials.itemname", getFinalArg(args, 0)).trim();
        if (name.isEmpty()) {
            name = null;
        }
        if (name == null) {
            item.remove(DataComponents.CUSTOM_NAME);
        } else {
            item.set(DataComponents.CUSTOM_NAME, Text.get().legacy(name));
        }
        Inventories.update(user.getBase());
        user.sendTl(name == null ? "itemnameClear" : "itemnameSuccess", name);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final ItemStack item = Inventories.getItemInHand(user.getBase());
            if (item != null && !item.isEmpty()) {
                final Component name = item.get(DataComponents.CUSTOM_NAME);
                if (name != null) {
                    return new ArrayList<>(List.of(FormatUtil.unformatString(user, "essentials.itemname", Text.get().nativeToLegacy(name))));
                }
            }
        }
        return Collections.emptyList();
    }
}
