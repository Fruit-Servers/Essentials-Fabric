package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignGrindstone extends EssentialsSign {
    public SignGrindstone() {
        super("Grindstone");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openGrindstone(player.getBase());
        return true;
    }
}
