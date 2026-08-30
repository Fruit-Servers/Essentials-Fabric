package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.TextInput;
import net.essentialsx.fabric.textreader.TextPager;

import java.io.IOException;

public class SignInfo extends EssentialsSign {
    public SignInfo() {
        super("Info");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final Trade charge = getTrade(sign, 3, ess);
        charge.isAffordableFor(player);
        final String chapter = sign.getLine(1);
        final String page = sign.getLine(2);
        final IText input;
        try {
            player.setDisplayNick();
            input = new TextInput(player.getSource(), "info", true, ess);
            final IText output = new KeywordReplacer(input, player.getSource(), ess);
            final TextPager pager = new TextPager(output);
            pager.showPage(chapter, page, null, player.getSource());
        } catch (final IOException ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        charge.charge(player);
        Trade.log("Sign", "Info", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
