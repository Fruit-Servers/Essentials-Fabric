package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Locale;

public class SignGameMode extends EssentialsSign {
    public SignGameMode() {
        super("GameMode");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        final String gamemode = sign.getLine(1);
        if (gamemode.isEmpty()) {
            sign.setLine(1, "Survival");
        }
        validateTrade(sign, 2, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final Trade charge = getTrade(sign, 2, ess);
        final String mode = sign.getLine(1).trim();
        if (mode.isEmpty()) {
            throw new SignException("invalidSignLine", 2);
        }
        charge.isAffordableFor(player);
        performSetMode(mode.toLowerCase(Locale.ENGLISH), player.getBase());
        player.sendTl("gameMode", player.playerTl(player.getBase().gameMode.getGameModeForPlayer().getName().toLowerCase(Locale.ENGLISH)), player.getDisplayName());
        Trade.log("Sign", "gameMode", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
        charge.charge(player);
        return true;
    }

    private void performSetMode(final String mode, final ServerPlayer player) throws SignException {
        if (mode.contains("survi") || mode.equalsIgnoreCase("0")) {
            player.setGameMode(GameType.SURVIVAL);
        } else if (mode.contains("creat") || mode.equalsIgnoreCase("1")) {
            player.setGameMode(GameType.CREATIVE);
        } else if (mode.contains("advent") || mode.equalsIgnoreCase("2")) {
            player.setGameMode(GameType.ADVENTURE);
        } else if (mode.contains("spec") || mode.equalsIgnoreCase("3")) {
            player.setGameMode(GameType.SPECTATOR);
        } else {
            throw new SignException("invalidSignLine", 2);
        }
    }
}
