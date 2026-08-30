package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

public class SignAnvil extends EssentialsSign {
    public SignAnvil() {
        super("Anvil");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        net.essentialsx.fabric.items.Workstations.openAnvil(player.getBase());
        return true;
    }
}
