package net.essentialsx.fabric.config;

import net.essentialsx.fabric.user.LazyLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * YAML-backed configuration/data file compatible with EssentialsX file formats.
 * Reads happen synchronously at load; writes are queued through {@link AsyncWriter}.
 * Unknown keys are preserved on round-trip.
 */
public class YamlFile {
    private static final Logger LOGGER = LoggerFactory.getLogger("Essentials-Config");
    private static final ThreadLocal<Yaml> YAML = ThreadLocal.withInitial(YamlFile::newYaml);

    private final Path file;
    private final String defaultResource;
    private final String header;
    private final Object lock = new Object();
    private final AtomicLong revision = new AtomicLong();
    private Map<String, Object> root = new LinkedHashMap<>();
    private boolean transaction;
    private boolean dirtyDuringTransaction;
    private Runnable saveHook;

    public YamlFile(final Path file) {
        this(file, null, null);
    }

    public YamlFile(final Path file, final String defaultResource) {
        this(file, defaultResource, null);
    }

    public YamlFile(final Path file, final String defaultResource, final String header) {
        this.file = file;
        this.defaultResource = defaultResource;
        this.header = header;
    }

    private static Yaml newYaml() {
        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(50);
        loaderOptions.setNestingDepthLimit(64);
        loaderOptions.setCodePointLimit(64 * 1024 * 1024);
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setAllowUnicode(true);
        final Representer representer = new Representer(dumperOptions);
        return new Yaml(new SafeConstructor(loaderOptions), representer, dumperOptions, loaderOptions);
    }

    public Path getFile() {
        return file;
    }

    public long getRevision() {
        return revision.get();
    }

    public void setSaveHook(final Runnable saveHook) {
        this.saveHook = saveHook;
    }

    /**
     * Load the file from disk, creating it from the bundled default resource when absent.
     */
    public void load() {
        synchronized (lock) {
            try {
                if (!Files.exists(file)) {
                    if (defaultResource != null) {
                        copyDefault();
                    } else {
                        root = new LinkedHashMap<>();
                        return;
                    }
                }
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    final Object loaded = YAML.get().load(reader);
                    if (loaded instanceof Map) {
                        //noinspection unchecked
                        root = new LinkedHashMap<>((Map<String, Object>) loaded);
                    } else {
                        root = new LinkedHashMap<>();
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Failed to load {}: {}", file, ex.getMessage());
                if (root == null) {
                    root = new LinkedHashMap<>();
                }
            }
        }
    }

    private void copyDefault() throws IOException {
        try (InputStream in = YamlFile.class.getResourceAsStream(defaultResource)) {
            if (in == null) {
                LOGGER.warn("Bundled default {} is missing", defaultResource);
                return;
            }
            Files.createDirectories(file.getParent());
            Files.copy(in, file);
        }
    }

    public boolean exists() {
        return Files.exists(file);
    }

    // ------------------------------------------------------------------ readers

    public Object get(final String path) {
        synchronized (lock) {
            return getRaw(path);
        }
    }

    private Object getRaw(final String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        Object current = root;
        for (final String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public boolean hasProperty(final String path) {
        return get(path) != null;
    }

    public boolean isList(final String path) {
        return get(path) instanceof List;
    }

    public boolean isBoolean(final String path) {
        return get(path) instanceof Boolean;
    }

    public boolean isSection(final String path) {
        return get(path) instanceof Map;
    }

    public String getString(final String path, final String def) {
        final Object value = get(path);
        return value == null ? def : value.toString();
    }

    public int getInt(final String path, final int def) {
        final Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (final NumberFormatException ignored) {
            }
        }
        return def;
    }

    public long getLong(final String path, final long def) {
        final Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (final NumberFormatException ignored) {
            }
        }
        return def;
    }

    public double getDouble(final String path, final double def) {
        final Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (final NumberFormatException ignored) {
            }
        }
        return def;
    }

    public float getFloat(final String path, final float def) {
        return (float) getDouble(path, def);
    }

    public boolean getBoolean(final String path, final boolean def) {
        final Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            final String s = ((String) value).trim();
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            if (s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return def;
    }

    public BigDecimal getBigDecimal(final String path, final BigDecimal def) {
        final Object value = get(path);
        if (value == null) {
            return def;
        }
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Double || value instanceof Float) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            }
            return new BigDecimal(value.toString().trim());
        } catch (final NumberFormatException ex) {
            return def;
        }
    }

    public List<String> getStringList(final String path) {
        final Object value = get(path);
        final List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (final Object o : (List<?>) value) {
                if (o != null) {
                    result.add(o.toString());
                }
            }
        } else if (value instanceof String && !((String) value).isEmpty()) {
            result.add((String) value);
        }
        return result;
    }

    public List<Object> getList(final String path) {
        final Object value = get(path);
        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
        }
        return new ArrayList<>();
    }

    /**
     * Returns a snapshot copy of the section as a map (or null).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(final String path) {
        synchronized (lock) {
            final Object value = getRaw(path);
            if (value instanceof Map) {
                return deepCopy((Map<String, Object>) value);
            }
            return null;
        }
    }

    public Set<String> getKeys(final String path) {
        final Map<String, Object> section = getSection(path);
        return section == null ? Collections.emptySet() : new LinkedHashSet<>(section.keySet());
    }

    public Set<String> getKeys() {
        synchronized (lock) {
            return new LinkedHashSet<>(root.keySet());
        }
    }

    public LazyLocation getLocation(final String path) {
        final Map<String, Object> section = getSection(path);
        if (section == null) {
            return null;
        }
        return LazyLocation.fromMap(section);
    }

    // ------------------------------------------------------------------ writers

    @SuppressWarnings("unchecked")
    public void setProperty(final String path, Object value) {
        if (value instanceof LazyLocation) {
            value = ((LazyLocation) value).toMap();
        }
        synchronized (lock) {
            if (path == null || path.isEmpty()) {
                if (value instanceof Map) {
                    root.putAll((Map<String, Object>) value);
                }
                return;
            }
            final String[] parts = path.split("\\.");
            Map<String, Object> current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                Object next = current.get(parts[i]);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(parts[i], next);
                }
                current = (Map<String, Object>) next;
            }
            if (value == null) {
                current.remove(parts[parts.length - 1]);
            } else {
                current.put(parts[parts.length - 1], normalize(value));
            }
            markDirty();
        }
    }

    public void removeProperty(final String path) {
        setProperty(path, null);
    }

    public void setRoot(final Map<String, Object> newRoot) {
        synchronized (lock) {
            root = new LinkedHashMap<>(newRoot);
            markDirty();
        }
    }

    public Map<String, Object> getRoot() {
        synchronized (lock) {
            return deepCopy(root);
        }
    }

    public void mutate(final Consumer<Map<String, Object>> mutator) {
        synchronized (lock) {
            mutator.accept(root);
            markDirty();
        }
    }

    private void markDirty() {
        revision.incrementAndGet();
        if (transaction) {
            dirtyDuringTransaction = true;
        }
    }

    // ------------------------------------------------------------------ saving

    public void startTransaction() {
        synchronized (lock) {
            transaction = true;
        }
    }

    public void stopTransaction() {
        stopTransaction(false);
    }

    public void stopTransaction(final boolean blocking) {
        boolean needsSave;
        synchronized (lock) {
            transaction = false;
            needsSave = dirtyDuringTransaction;
            dirtyDuringTransaction = false;
        }
        if (needsSave) {
            if (blocking) {
                blockingSave();
            } else {
                save();
            }
        }
    }

    public CompletableFuture<Void> save() {
        final String content;
        synchronized (lock) {
            if (transaction) {
                dirtyDuringTransaction = true;
                return CompletableFuture.completedFuture(null);
            }
            content = encode();
        }
        return AsyncWriter.write(file, content);
    }

    public void blockingSave() {
        final String content;
        synchronized (lock) {
            transaction = false;
            dirtyDuringTransaction = false;
            content = encode();
        }
        try {
            AsyncWriter.writeBlocking(file, content);
        } catch (final IOException ex) {
            LOGGER.error("Failed to save {}", file, ex);
        }
    }

    private String encode() {
        if (saveHook != null) {
            saveHook.run();
        }
        final StringBuilder sb = new StringBuilder();
        if (header != null && !header.isEmpty()) {
            for (final String line : header.split("\n")) {
                sb.append("# ").append(line).append('\n');
            }
        }
        sb.append(YAML.get().dump(deepCopy(root)));
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(final Map<String, Object> map) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopyValue(final Object value) {
        if (value instanceof Map) {
            return deepCopy((Map<String, Object>) value);
        }
        if (value instanceof List) {
            final List<Object> list = new ArrayList<>();
            for (final Object o : (List<?>) value) {
                list.add(deepCopyValue(o));
            }
            return list;
        }
        return value;
    }

    /**
     * Convert values into plain YAML-representable types.
     */
    @SuppressWarnings("unchecked")
    private static Object normalize(final Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof java.util.UUID || value instanceof Enum) {
            return value.toString();
        }
        if (value instanceof LazyLocation) {
            return ((LazyLocation) value).toMap();
        }
        if (value instanceof Map) {
            final Map<String, Object> map = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                map.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return map;
        }
        if (value instanceof Set) {
            final List<Object> list = new ArrayList<>();
            for (final Object o : (Set<?>) value) {
                list.add(normalize(o));
            }
            return list;
        }
        if (value instanceof List) {
            final List<Object> list = new ArrayList<>();
            for (final Object o : (List<?>) value) {
                list.add(normalize(o));
            }
            return list;
        }
        return value;
    }
}
