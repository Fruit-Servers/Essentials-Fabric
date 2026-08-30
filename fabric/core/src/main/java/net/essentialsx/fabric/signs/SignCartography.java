package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignCartography extends EssentialsSign {
    public SignCartography() {
        super("Cartography");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openCartography(player.getBase());
        return true;
    }
}
