package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.essentialsx.fabric.items.Inventories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemTestCommand extends EssentialsTreeNode {
    public ItemTestCommand() {
        super(new String[] {"itemtest"}, true);
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (!sender.isAuthorized("essentials.itemtest") || args.length < 1 || !sender.isPlayer()) {
            return;
        }
        final ServerPlayer player = sender.getPlayer();
        switch (args[0]) {
            case "slot": {
                if (args.length < 2) {
                    return;
                }
                player.getInventory().setItem(Integer.parseInt(args[1]), new ItemStack(Items.DIRT));
                Inventories.update(player);
                break;
            }
            case "overfill": {
                sender.sendMessage(Inventories.addItem(player, 42, false, new ItemStack(Items.DIAMOND_SWORD, 1), new ItemStack(Items.DIRT, 32), new ItemStack(Items.DIRT, 32)).toString());
                Inventories.update(player);
                break;
            }
            case "overfill2": {
                if (args.length < 3) {
                    return;
                }
                final boolean armor = Boolean.parseBoolean(args[1]);
                final boolean add = Boolean.parseBoolean(args[2]);
                final ItemStack[] items = new ItemStack[] {new ItemStack(Items.DIAMOND_SWORD, 1), new ItemStack(Items.DIRT, 32), new ItemStack(Items.DIRT, 32), new ItemStack(Items.DIAMOND_HELMET, 4), new ItemStack(Items.CHAINMAIL_LEGGINGS, 1)};
                if (Inventories.hasSpace(player, 0, armor, items)) {
                    if (add) {
                        sender.sendMessage(Inventories.addItem(player, 0, armor, items).toString());
                        Inventories.update(player);
                    }
                    sender.sendMessage("SO MUCH SPACE!");
                } else {
                    sender.sendMessage("No space!");
                }
                break;
            }
            case "remove": {
                if (args.length < 2) {
                    return;
                }
                Inventories.removeItemExact(player, new ItemStack(Items.PUMPKIN, 1), 1);
                Inventories.update(player);
                break;
            }
            default: {
                break;
            }
        }
    }
}
