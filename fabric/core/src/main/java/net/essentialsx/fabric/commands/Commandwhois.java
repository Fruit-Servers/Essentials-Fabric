package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.SetExpFix;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.Collections;
import java.util.List;

public class Commandwhois extends EssentialsCommand {
    public Commandwhois() {
        super("whois");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, sender, args, 0);
        final ServerPlayer base = user.getBase();
        sender.sendTl("whoisTop", user.getName());
        user.setDisplayNick();
        sender.sendTl("whoisNick", user.getDisplayName());
        sender.sendTl("whoisUuid", user.getUUID().toString());
        sender.sendTl("whoisHealth", (double) base.getHealth());
        sender.sendTl("whoisHunger", base.getFoodData().getFoodLevel(), base.getFoodData().getSaturationLevel());
        sender.sendTl("whoisExp", SetExpFix.getTotalExperience(base), base.experienceLevel);
        sender.sendTl("whoisLocation", user.getLocation().worldDisplayName(server), user.getLocation().blockX(), user.getLocation().blockY(), user.getLocation().blockZ());
        final long playtimeMs = System.currentTimeMillis() - (base.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) * 50L);
        sender.sendTl("whoisPlaytime", DateUtil.formatDateDiff(playtimeMs));
        if (!ess.getSettings().isEcoDisabled()) {
            sender.sendTl("whoisMoney", Text.parsed(NumberUtil.displayCurrency(user.getMoney(), ess)));
        }
        if (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.whois.ip")) {
            sender.sendTl("whoisIPAddress", user.getIpAddress());
        }
        final String location = user.getGeoLocation();
        if (location != null && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.geoip.show"))) {
            sender.sendTl("whoisGeoLocation", location);
        }
        sender.sendTl("whoisGamemode", sender.tl(Commandgamemode.modeName(base.gameMode.getGameModeForPlayer())));
        sender.sendTl("whoisGod", CommonPlaceholders.trueFalse(sender, user.isGodModeEnabled()));
        sender.sendTl("whoisOp", CommonPlaceholders.trueFalse(sender, user.isOp()));
        sender.sendTl("whoisFly", CommonPlaceholders.trueFalse(sender, base.getAbilities().mayfly), Text.parsed(base.getAbilities().flying ? sender.tl("flying") : sender.tl("notFlying")));
        sender.sendTl("whoisSpeed", base.getAbilities().flying ? base.getAbilities().getFlyingSpeed() : base.getAbilities().getWalkingSpeed());
        final boolean whitelisted = server.getPlayerList().getWhiteList().isWhiteListed(Bans.profile(ess, user.getUUID(), user.getName()));
        sender.sendTl("whoisWhitelist", CommonPlaceholders.trueFalse(sender, whitelisted));
        if (user.isAfk()) {
            sender.sendTl("whoisAFKSince", CommonPlaceholders.trueFalse(sender, true), DateUtil.formatDateDiff(user.getAfkSince()));
        } else {
            sender.sendTl("whoisAFK", CommonPlaceholders.trueFalse(sender, false));
        }
        sender.sendTl("whoisJail", Text.parsed(user.isJailed() ? user.getJailTimeout() > 0 ? user.getFormattedJailTime() : sender.tl("true") : sender.tl("false")));
        final long muteTimeout = user.getMuteTimeout();
        if (!user.hasMuteReason()) {
            sender.sendTl("whoisMuted", Text.parsed(user.isMuted() ? muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : sender.tl("true") : sender.tl("false")));
        } else {
            sender.sendTl("whoisMutedReason", Text.parsed(user.isMuted() ? muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : sender.tl("true") : sender.tl("false")), user.getMuteReason());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
