package net.essentialsx.fabric.textreader;

import net.essentialsx.fabric.Essentials;

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

public class BookInput implements IText {
    private static final HashMap<String, SoftReference<BookInput>> cache = new HashMap<>();
    private final transient List<String> lines;
    private final transient List<String> chapters;
    private final transient Map<String, Integer> bookmarks;
    private final transient long lastChange;

    public BookInput(final String filename, final boolean createFile, final Essentials ess) throws IOException {
        final Path file = ess.getDataFolder().resolve(filename + ".txt");
        if (!Files.exists(file) && createFile) {
            try (InputStream input = BookInput.class.getResourceAsStream("/" + filename + ".txt")) {
                if (input != null) {
                    Files.createDirectories(file.getParent());
                    Files.copy(input, file);
                }
            }
            ess.getLogger().info("File " + filename + ".txt does not exist. Creating one for you.");
        }
        if (!Files.exists(file)) {
            lastChange = 0;
            lines = Collections.emptyList();
            chapters = Collections.emptyList();
            bookmarks = Collections.emptyMap();
            throw new FileNotFoundException("Could not create " + filename + ".txt");
        } else {
            lastChange = Files.getLastModifiedTime(file).toMillis();
            final boolean readFromfile;
            final String key = file.getFileName().toString();
            synchronized (cache) {
                final SoftReference<BookInput> inputRef = cache.get(key);
                final BookInput input;
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
                        if (line.length() > 0 && line.charAt(0) == '#') {
                            bookmarks.put(line.substring(1).toLowerCase(Locale.ENGLISH).replaceAll("&[0-9a-fk]", ""), lineNumber);
                            chapters.add(line.substring(1).replace('&', '§').replace("§§", "&"));
                        }
                        lines.add(line.replace('&', '§').replace("§§", "&"));
                        lineNumber++;
                    }
                }
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
