package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandbook extends EssentialsCommand {
    public Commandbook() {
        super("book");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack item = Inventories.getItemInMainHand(user.getBase());
        final String player = user.getName();
        if (item.is(Items.WRITTEN_BOOK)) {
            final WrittenBookContent bmeta = item.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
            if (args.length > 1 && args[0].equalsIgnoreCase("author")) {
                if (user.isAuthorized("essentials.book.author") && (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others"))) {
                    final String newAuthor = FormatUtil.formatString(user, "essentials.book.author", getFinalArg(args, 1)).trim();
                    item.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(bmeta.title(), newAuthor, bmeta.generation(), bmeta.pages(), bmeta.resolved()));
                    user.sendTl("bookAuthorSet", newAuthor);
                } else {
                    throw new TranslatableException("denyChangeAuthor");
                }
            } else if (args.length > 1 && args[0].equalsIgnoreCase("title")) {
                if (user.isAuthorized("essentials.book.title") && (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others"))) {
                    final String newTitle = FormatUtil.formatString(user, "essentials.book.title", getFinalArg(args, 1)).trim();
                    item.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(newTitle), bmeta.author(), bmeta.generation(), bmeta.pages(), bmeta.resolved()));
                    user.sendTl("bookTitleSet", newTitle);
                } else {
                    throw new TranslatableException("denyChangeTitle");
                }
            } else {
                if (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others")) {
                    final ItemStack newItem = new ItemStack(Items.WRITABLE_BOOK, item.getCount());
                    final List<Filterable<String>> pages = new ArrayList<>();
                    for (final Filterable<Component> page : bmeta.pages()) {
                        pages.add(Filterable.passThrough(Text.get().nativeToLegacy(page.raw())));
                    }
                    newItem.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
                    Inventories.setItemInMainHand(user.getBase(), newItem);
                    user.sendTl("editBookContents");
                } else {
                    throw new TranslatableException("denyBookEdit");
                }
            }
            Inventories.update(user.getBase());
        } else if (item.is(Items.WRITABLE_BOOK)) {
            final WritableBookContent bmeta = item.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
            final ItemStack newItem = new ItemStack(Items.WRITTEN_BOOK, item.getCount());
            final List<Filterable<Component>> pages = new ArrayList<>();
            for (final Filterable<String> page : bmeta.pages()) {
                pages.add(Filterable.passThrough(Text.get().legacy(page.raw())));
            }
            final String author = user.isAuthorized("essentials.book.author") && item.has(DataComponents.CUSTOM_NAME) ? player : player;
            final String title = item.has(DataComponents.CUSTOM_NAME) ? item.getHoverName().getString() : "";
            newItem.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, true));
            Inventories.setItemInMainHand(user.getBase(), newItem);
            Inventories.update(user.getBase());
            user.sendTl("bookLocked");
        } else {
            throw new TranslatableException("holdBook");
        }
    }

    private boolean isAuthor(final WrittenBookContent bmeta, final String player) {
        final String author = bmeta.author();
        return author != null && author.equalsIgnoreCase(player);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("sign", "unsign"));
            if (user.isAuthorized("essentials.book.author")) {
                options.add("author");
            }
            if (user.isAuthorized("essentials.book.title")) {
                options.add("title");
            }
            return options;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("author") && user.isAuthorized("essentials.book.author")) {
            final List<String> options = getPlayers(user);
            options.add("Herobrine");
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
