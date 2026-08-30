package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.economy.Trade;
import net.minecraft.world.item.ItemStack;

public class SignFree extends EssentialsSign {
    public SignFree() {
        super("Free");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        try {
            ItemStack item = getItemStack(sign.getLine(1), 1, ess);
            item = getItemMeta(item, sign.getLine(2), ess);
            item = getItemMeta(item, sign.getLine(3), ess);
        } catch (final SignException ex) {
            sign.setLine(1, "\u00a7c<item>");
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        ItemStack itemStack = getItemStack(sign.getLine(1), 1, ess);
        itemStack = getItemMeta(player.getSource(), itemStack, sign.getLine(2), ess);
        final ItemStack item = getItemMeta(player.getSource(), itemStack, sign.getLine(3), ess);
        if (item.isEmpty()) {
            throw new SignException("cantSpawnItem", "Air");
        }
        item.setCount(item.getMaxStackSize());
        final net.minecraft.network.chat.Component displayName = item.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? item.getHoverName() : net.minecraft.network.chat.Component.literal(net.essentialsx.fabric.user.UserData.itemKey(item));
        net.essentialsx.fabric.items.Workstations.openFree(player.getBase(), displayName, item);
        Trade.log("Sign", "Free", "Interact", username, null, username, new Trade(item, ess), sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
