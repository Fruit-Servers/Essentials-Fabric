package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignLoom extends EssentialsSign {
    public SignLoom() {
        super("Loom");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openLoom(player.getBase());
        return true;
    }
}
