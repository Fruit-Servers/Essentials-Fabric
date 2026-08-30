package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignSmithing extends EssentialsSign {
    public SignSmithing() {
        super("Smithing");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openSmithing(player.getBase());
        return true;
    }
}
