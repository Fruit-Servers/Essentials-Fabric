package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Commandcreatekit extends EssentialsCommand {
    public Commandcreatekit() {
        super("createkit");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length != 2) {
            throw new NotEnoughArgumentsException();
        }
        final long delay = Long.parseLong(args[1]);
        final String kitname = args[0];
        final ItemStack[] items = Inventories.getInventory(user.getBase(), true);
        final List<String> list = new ArrayList<>();
        final boolean useSerialization = ess.getSettings().isUseBetterKits();
        for (int i = 0; i < items.length; i++) {
            final ItemStack is = items[i];
            if (is != null && !is.isEmpty()) {
                final String serialized;
                if (useSerialization) {
                    serialized = "slot:" + i + " @" + ess.getItemSerializer().serialize(is);
                } else {
                    serialized = "slot:" + i + " " + ess.getItemDb().serialize(is);
                }
                list.add(serialized);
            }
        }
        // Pastebin upload is not supported on Fabric (Section 21.1: no outbound uploads by default).
        ess.getKits().addKit(kitname, list, delay);
        user.sendTl("createdKit", kitname, list.size(), delay);
    }
}
