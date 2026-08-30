package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.utils.NumberUtil;

public class SignBalance extends EssentialsSign {
    public SignBalance() {
        super("Balance");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        player.sendTl("balance", Text.parsed(NumberUtil.displayCurrency(player.getMoney(), ess)));
        return true;
    }
}
