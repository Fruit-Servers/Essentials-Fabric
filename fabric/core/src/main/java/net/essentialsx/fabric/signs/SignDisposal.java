package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;

public class SignDisposal extends EssentialsSign {
    public SignDisposal() {
        super("Disposal");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        if (!player.isAuthorized("essentials.signs.disposal.name")) {
            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");
        }
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) {
        String title = (sign.getLine(1) + " " + sign.getLine(2) + " " + sign.getLine(3)).trim();
        if (title.isEmpty()) {
            title = player.playerTl("disposal");
        }
        net.essentialsx.fabric.items.Workstations.openDisposal(player.getBase(), net.essentialsx.fabric.text.Text.get().legacy(title));
        return true;
    }
}
