package net.essentialsx.fabric.textreader;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.StringUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads {@code <name>.txt} pages (motd/rules/info/custom) with per-player and per-group
 * overrides, cached by file modification time.
 */
public class TextInput implements IText {
    private static final HashMap<String, SoftReference<TextInput>> cache = new HashMap<>();
    private final transient List<String> lines;
    private final transient List<String> chapters;
    private final transient Map<String, Integer> bookmarks;
    private final transient long lastChange;

    public TextInput(final CommandSource sender, final String filename, final boolean createFile, final Essentials ess) throws IOException {
        Path file = null;
        if (sender.isPlayer()) {
            final User user = ess.getUser(sender.getPlayer());
            file = ess.getDataFolder().resolve(filename + "_" + StringUtil.sanitizeFileName(user.getName()) + ".txt");
            if (!Files.exists(file)) {
                file = ess.getDataFolder().resolve(filename + "_" + StringUtil.sanitizeFileName(user.getGroup()) + ".txt");
            }
        }
        if (file == null || !Files.exists(file)) {
            file = ess.getDataFolder().resolve(filename + ".txt");
        }
        if (Files.exists(file)) {
            lastChange = Files.getLastModifiedTime(file).toMillis();
            final boolean readFromfile;
            final String key = file.getFileName().toString();
            synchronized (cache) {
                final SoftReference<TextInput> inputRef = cache.get(key);
                final TextInput input;
                if (inputRef == null || (input = inputRef.get()) == null || input.lastChange < lastChange) {
                    lines = new ArrayList<>();
                    chapters = new ArrayList<>();
                    bookmarks = new HashMap<>();
                    cache.put(key, new SoftReference<>(this));
                    readFromfile = true;
                } else {
                    lines = Collections.unmodifiableList(input.getLines());
                    chapters = Collections.unmodifiableList(input.getChapters());
                    bookmarks = Collections.unmodifiableMap(input.getBookmarks());
                    readFromfile = false;
                }
            }
            if (readFromfile) {
                try (BufferedReader bufferedReader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    int lineNumber = 0;
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.length() > 1 && line.charAt(0) == '#') {
                            final String[] titles = line.substring(1).trim().replace(" ", "_").split(",");
                            chapters.add(FormatUtil.replaceFormat(titles[0]));
                            for (final String title : titles) {
                                bookmarks.put(FormatUtil.stripEssentialsFormat(title.toLowerCase(Locale.ENGLISH)), lineNumber);
                            }
                        }
                        lines.add(FormatUtil.replaceFormat(line));
                        lineNumber++;
                    }
                }
            }
        } else {
            lastChange = 0;
            lines = Collections.emptyList();
            chapters = Collections.emptyList();
            bookmarks = Collections.emptyMap();
            if (createFile) {
                try (InputStream input = TextInput.class.getResourceAsStream("/" + filename + ".txt")) {
                    if (input != null) {
                        Files.createDirectories(file.getParent());
                        Files.copy(input, file);
                    }
                }
                throw new FileNotFoundException("File " + filename + ".txt does not exist. Creating one for you.");
            }
        }
    }

    @Override
    public List<String> getLines() {
        return lines;
    }

    @Override
    public List<String> getChapters() {
        return chapters;
    }

    @Override
    public Map<String, Integer> getBookmarks() {
        return bookmarks;
    }
}
