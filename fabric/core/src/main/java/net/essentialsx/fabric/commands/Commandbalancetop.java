package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.economy.BalanceTop;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.textreader.SimpleTextInput;
import net.essentialsx.fabric.textreader.TextPager;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

public class Commandbalancetop extends EssentialsCommand {
    public static final int MINUSERS = 50;
    private static final int CACHETIME = 2 * 60 * 1000;
    private static SimpleTextInput cache = new SimpleTextInput();

    public Commandbalancetop() {
        super("balancetop");
    }

    private void outputCache(final CommandSource sender, final int page) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ess.getBalanceTop().getCacheAge());
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        ess.scheduleSyncDelayedTask(() -> {
            sender.sendTl("balanceTop", format.format(cal.getTime()));
            new TextPager(cache).showPage(Integer.toString(page), null, "balancetop", sender);
        });
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        int page = 0;
        boolean force = false;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                if (args[0].equalsIgnoreCase("force") && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.balancetop.force"))) {
                    force = true;
                }
            }
        }
        if (!force && ess.getBalanceTop().getCacheAge() > System.currentTimeMillis() - CACHETIME) {
            outputCache(sender, page);
            return;
        }
        if (ess.getUsers().getUserCount() > MINUSERS) {
            sender.sendTl("orderBalances", ess.getUsers().getUserCount());
        }
        view(sender, page, force);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("1"));
            if (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.balancetop.force")) {
                options.add("force");
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }

    private void view(final CommandSource sender, final int page, final boolean force) {
        if (ess.getSettings().isEcoDisabled()) {
            return;
        }
        final boolean fresh = force || ess.getBalanceTop().isCacheLocked() || ess.getBalanceTop().getCacheAge() <= System.currentTimeMillis() - CACHETIME;
        final CompletableFuture<Void> future = fresh ? ess.getBalanceTop().calculateBalanceTopMapAsync() : CompletableFuture.completedFuture(null);
        future.thenRun(() -> {
            if (fresh) {
                final SimpleTextInput newCache = new SimpleTextInput();
                newCache.getLines().add(Text.get().miniToLegacy(tlLiteral("serverTotal", Text.parsed(NumberUtil.displayCurrency(ess.getBalanceTop().getBalanceTopTotal(), ess)))));
                int pos = 1;
                for (final Map.Entry<UUID, BalanceTop.Entry> entry : ess.getBalanceTop().getBalanceTopCache().entrySet()) {
                    newCache.getLines().add(Text.get().miniToLegacy(tlLiteral("balanceTopLine", pos, entry.getValue().name(), Text.parsed(NumberUtil.displayCurrency(entry.getValue().balance(), ess)))));
                    pos++;
                }
                cache = newCache;
            }
            outputCache(sender, page);
        }).exceptionally(t -> {
            ess.showError(sender, t, "balancetop");
            return null;
        });
    }
}
