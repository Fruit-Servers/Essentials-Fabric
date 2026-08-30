package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.textreader.SimpleTextPager;
import net.essentialsx.fabric.textreader.SimpleTranslatableText;
import net.essentialsx.fabric.user.MailMessage;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class Commandmail extends EssentialsCommand {
    private static int mailsPerMinute = 0;
    private static long timestamp = 0;

    public Commandmail() {
        super("mail");
    }

    private static void checkMuted(final User user) throws TranslatableException {
        if (user.isMuted()) {
            final String dateDiff = user.getMuteTimeout() > 0 ? DateUtil.formatDateDiff(user.getMuteTimeout()) : null;
            if (dateDiff == null) {
                throw new TranslatableException(user.hasMuteReason() ? "voiceSilencedReason" : "voiceSilenced", user.getMuteReason());
            }
            throw new TranslatableException(user.hasMuteReason() ? "voiceSilencedReasonTime" : "voiceSilencedTime", dateDiff, user.getMuteReason());
        }
    }

    private void checkRate() throws TranslatableException {
        if (Math.abs(System.currentTimeMillis() - timestamp) > 60000) {
            timestamp = System.currentTimeMillis();
            mailsPerMinute = 0;
        }
        mailsPerMinute++;
        if (mailsPerMinute > ess.getSettings().getMailsPerMinute()) {
            throw new TranslatableException("mailDelay", ess.getSettings().getMailsPerMinute());
        }
    }

    private User target(final MinecraftServer server, final String name) throws Exception {
        try {
            return getPlayer(server, name, true, true);
        } catch (final PlayerNotFoundException e) {
            throw new TranslatableException("playerNeverOnServer", name);
        }
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length >= 1 && "read".equalsIgnoreCase(args[0])) {
            final List<MailMessage> mail = user.getMailMessages();
            if (mail == null || mail.size() == 0) {
                user.sendTl("noMail");
                throw new NoChargeException();
            }
            final SimpleTranslatableText input = new SimpleTranslatableText();
            final ListIterator<MailMessage> iterator = mail.listIterator();
            while (iterator.hasNext()) {
                final MailMessage mailObj = iterator.next();
                if (mailObj.isExpired()) {
                    iterator.remove();
                    continue;
                }
                input.addLine(ess.getMail().getMailTlKey(mailObj), ess.getMail().getMailTlArgs(mailObj));
                iterator.set(mailObj.asRead());
            }
            if (input.getLines().isEmpty()) {
                user.sendTl("noMail");
                throw new NoChargeException();
            }
            final SimpleTextPager pager = new SimpleTextPager(input);
            pager.showPage(user.getSource(), args.length > 1 ? args[1] : null, commandLabel + " " + args[0]);
            user.sendTl("mailClear");
            user.setMailList(mail);
            return;
        }
        if (args.length >= 3 && "send".equalsIgnoreCase(args[0])) {
            if (!user.isAuthorized("essentials.mail.send")) {
                throw new TranslatableException("noPerm", "essentials.mail.send");
            }
            checkMuted(user);
            final User u = target(server, args[1]);
            final String msg = FormatUtil.formatMessage(user, "essentials.mail", StringUtil.sanitizeString(FormatUtil.stripFormat(getFinalArg(args, 2))));
            if (msg.length() > 1000) {
                throw new TranslatableException("mailTooLong");
            }
            if (!u.isIgnoredPlayer(user)) {
                checkRate();
                u.sendMail(user, msg);
            }
            user.sendTl("mailSentTo", u.getDisplayName(), u.getName());
            user.sendMessage(msg);
            return;
        }
        if (args.length >= 4 && "sendtemp".equalsIgnoreCase(args[0])) {
            if (!user.isAuthorized("essentials.mail.sendtemp")) {
                throw new TranslatableException("noPerm", "essentials.mail.sendtemp");
            }
            checkMuted(user);
            final User u = target(server, args[1]);
            final long dateDiff = DateUtil.parseDateDiff(args[2], true);
            final String msg = FormatUtil.formatMessage(user, "essentials.mail", StringUtil.sanitizeString(FormatUtil.stripFormat(getFinalArg(args, 3))));
            if (msg.length() > 1000) {
                throw new TranslatableException("mailTooLong");
            }
            if (!u.isIgnoredPlayer(user)) {
                checkRate();
                u.sendMail(user, msg, dateDiff);
            }
            user.sendTl("mailSentToExpire", u.getDisplayName(), DateUtil.formatDateDiff(dateDiff), u.getName());
            user.sendMessage(msg);
            return;
        }
        if (args.length > 1 && "sendall".equalsIgnoreCase(args[0])) {
            if (!user.isAuthorized("essentials.mail.sendall")) {
                throw new TranslatableException("noPerm", "essentials.mail.sendall");
            }
            ess.runTaskAsynchronously(new SendAll(user, FormatUtil.formatMessage(user, "essentials.mail", StringUtil.sanitizeString(FormatUtil.stripFormat(getFinalArg(args, 1)))), 0));
            user.sendTl("mailSent");
            return;
        }
        if (args.length >= 3 && "sendtempall".equalsIgnoreCase(args[0])) {
            if (!user.isAuthorized("essentials.mail.sendtempall")) {
                throw new TranslatableException("noPerm", "essentials.mail.sendtempall");
            }
            ess.runTaskAsynchronously(new SendAll(user, FormatUtil.formatMessage(user, "essentials.mail", StringUtil.sanitizeString(FormatUtil.stripFormat(getFinalArg(args, 2)))), DateUtil.parseDateDiff(args[1], true)));
            user.sendTl("mailSent");
            return;
        }
        if (args.length >= 1 && "clear".equalsIgnoreCase(args[0])) {
            User mailUser = user;
            int toRemove = -1;
            if (args.length > 1) {
                if (NumberUtil.isPositiveInt(args[1])) {
                    toRemove = Integer.parseInt(args[1]);
                } else if (!user.isAuthorized("essentials.mail.clear.others")) {
                    throw new TranslatableException("noPerm", "essentials.mail.clear.others");
                } else {
                    mailUser = getPlayer(server, user, args, 1, true);
                    if (args.length > 2 && NumberUtil.isPositiveInt(args[2])) {
                        toRemove = Integer.parseInt(args[2]);
                    }
                }
            }
            final List<MailMessage> mails = mailUser.getMailMessages();
            if (mails == null || mails.isEmpty()) {
                user.sendTl(mailUser == user ? "noMail" : "noMailOther", mailUser.getDisplayName());
                throw new NoChargeException();
            }
            if (toRemove > 0) {
                if (toRemove > mails.size()) {
                    user.sendTl("mailClearIndex", mails.size());
                    throw new NoChargeException();
                }
                mails.remove(toRemove - 1);
                mailUser.setMailList(mails);
            } else {
                mailUser.setMailList(null);
            }
            user.sendTl("mailCleared");
            return;
        }
        if (args.length >= 1 && "clearall".equalsIgnoreCase(args[0])) {
            if (!user.isAuthorized("essentials.mail.clearall")) {
                throw new TranslatableException("noPerm", "essentials.mail.clearall");
            }
            ess.runTaskAsynchronously(new ClearAll());
            user.sendTl("mailClearedAll");
            return;
        }
        throw new NotEnoughArgumentsException();
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length >= 1 && "read".equalsIgnoreCase(args[0])) {
            throw new TranslatableException("onlyPlayers", commandLabel + " read");
        } else if (args.length > 1 && "clear".equalsIgnoreCase(args[0])) {
            final User mailUser = getPlayer(server, args[1], true, true);
            final int toRemove = args.length > 2 ? NumberUtil.isPositiveInt(args[2]) ? Integer.parseInt(args[2]) : -1 : -1;
            final List<MailMessage> mails = mailUser.getMailMessages();
            if (mails == null || mails.isEmpty()) {
                sender.sendTl("noMailOther", mailUser.getDisplayName());
                throw new NoChargeException();
            }
            if (toRemove > 0) {
                if (toRemove > mails.size()) {
                    sender.sendTl("mailClearIndex", mails.size());
                    throw new NoChargeException();
                }
                mails.remove(toRemove - 1);
                mailUser.setMailList(mails);
            } else {
                mailUser.setMailList(null);
            }
            sender.sendTl("mailCleared");
            return;
        } else if (args.length >= 1 && "clearall".equalsIgnoreCase(args[0])) {
            ess.runTaskAsynchronously(new ClearAll());
            sender.sendTl("mailClearedAll");
            return;
        } else if (args.length >= 3 && "send".equalsIgnoreCase(args[0])) {
            final User u = target(server, args[1]);
            u.sendMail(Console.getInstance(), FormatUtil.replaceFormat(getFinalArg(args, 2)));
            sender.sendTl("mailSent");
            return;
        } else if (args.length >= 4 && "sendtemp".equalsIgnoreCase(args[0])) {
            final User u = target(server, args[1]);
            final long dateDiff = DateUtil.parseDateDiff(args[2], true);
            u.sendMail(Console.getInstance(), FormatUtil.replaceFormat(getFinalArg(args, 3)), dateDiff);
            sender.sendTl("mailSent");
            return;
        } else if (args.length >= 2 && "sendall".equalsIgnoreCase(args[0])) {
            ess.runTaskAsynchronously(new SendAll(Console.getInstance(), FormatUtil.replaceFormat(getFinalArg(args, 1)), 0));
            sender.sendTl("mailSent");
            return;
        } else if (args.length >= 3 && "sendtempall".equalsIgnoreCase(args[0])) {
            final long dateDiff = DateUtil.parseDateDiff(args[1], true);
            ess.runTaskAsynchronously(new SendAll(Console.getInstance(), FormatUtil.replaceFormat(getFinalArg(args, 2)), dateDiff));
            sender.sendTl("mailSent");
            return;
        } else if (args.length >= 2) {
            //allow sending from console without "send" argument, since it's the only thing the console can do
            final User u = target(server, args[0]);
            u.sendMail(Console.getInstance(), FormatUtil.replaceFormat(getFinalArg(args, 1)));
            sender.sendTl("mailSent");
            return;
        }
        throw new NotEnoughArgumentsException();
    }

    private class SendAll implements Runnable {
        private final IMessageRecipient messageRecipient;
        private final String message;
        private final long dateDiff;

        SendAll(final IMessageRecipient messageRecipient, final String message, final long dateDiff) {
            this.messageRecipient = messageRecipient;
            this.message = message;
            this.dateDiff = dateDiff;
        }

        @Override
        public void run() {
            for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                final User user = ess.getUsers().getUser(u);
                if (user != null) {
                    user.sendMail(messageRecipient, message, dateDiff);
                }
            }
        }
    }

    private class ClearAll implements Runnable {
        @Override
        public void run() {
            for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                final User user = ess.getUsers().getUser(u);
                if (user != null) {
                    user.setMailList(null);
                }
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("read", "clear"));
            if (user.isAuthorized("essentials.mail.send")) {
                options.add("send");
            }
            if (user.isAuthorized("essentials.mail.sendtemp")) {
                options.add("sendtemp");
            }
            if (user.isAuthorized("essentials.mail.sendall")) {
                options.add("sendall");
            }
            if (user.isAuthorized("essentials.mail.sendtempall")) {
                options.add("sendtempall");
            }
            if (user.isAuthorized("essentials.mail.clearall")) {
                options.add("clearall");
            }
            return options;
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("send") && user.isAuthorized("essentials.mail.send")) || (args[0].equalsIgnoreCase("sendtemp") && user.isAuthorized("essentials.mail.sendtemp")) || ((args[0].equalsIgnoreCase("clear")) && user.isAuthorized("essentials.mail.clear.others"))) {
                return getPlayers(user);
            } else if (args[0].equalsIgnoreCase("sendtempall") && user.isAuthorized("essentials.mail.sendtempall")) {
                return COMMON_DATE_DIFFS;
            } else if (args[0].equalsIgnoreCase("read")) {
                final List<MailMessage> mail = user.getMailMessages();
                final int pages = mail != null ? (mail.size() / 9 + (mail.size() % 9 > 0 ? 1 : 0)) : 0;
                if (pages == 0) {
                    return new ArrayList<>(List.of("0"));
                } else {
                    final List<String> options = new ArrayList<>();
                    for (int i = 0; i < pages; i++) {
                        options.add(String.valueOf(i + 1));
                    }
                    return options;
                }
            } else if (args[0].equalsIgnoreCase("clear")) {
                final List<MailMessage> mail = user.getMailMessages();
                final int size = mail == null ? 0 : mail.size();
                if (size >= 9) {
                    return new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"));
                } else {
                    final List<String> options = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        options.add(String.valueOf(i + 1));
                    }
                    return options;
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sendtemp") && user.isAuthorized("essentials.mail.sendtemp")) {
            return COMMON_DATE_DIFFS;
        }
        return Collections.emptyList();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("send", "sendall", "sendtemp", "sendtempall", "clearall", "clear"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("sendtemp") || args[0].equalsIgnoreCase("clear")) {
                return getPlayers(sender);
            } else if (args[0].equalsIgnoreCase("sendtempall")) {
                return COMMON_DATE_DIFFS;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sendtemp")) {
            return COMMON_DATE_DIFFS;
        }
        return Collections.emptyList();
    }
}
