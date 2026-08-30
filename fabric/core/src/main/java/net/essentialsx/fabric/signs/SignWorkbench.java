package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignWorkbench extends EssentialsSign {
    public SignWorkbench() {
        super("Workbench");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openWorkbench(player.getBase());
        return true;
    }
}
