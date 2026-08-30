package net.essentialsx.fabric.text;

import net.essentialsx.fabric.Essentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Locale bundle loader compatible with EssentialsX {@code messages_*.properties} files.
 * Bundles are MiniMessage formatted and use {@link MessageFormat} placeholders.
 */
public class I18n {
    private static final String MESSAGES = "messages";
    private static final Pattern NODOUBLEMARK = Pattern.compile("''");
    private static volatile ExecutorService BUNDLE_LOADER_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        final Thread t = new Thread(r, "Essentials-I18n");
        t.setDaemon(true);
        return t;
    });
    private static final ResourceBundle NULL_BUNDLE = new ResourceBundle() {
        public Enumeration<String> getKeys() {
            return null;
        }

        protected Object handleGetObject(final String key) {
            return null;
        }
    };
    private static I18n instance;
    private final transient Locale defaultLocale = Locale.getDefault();
    private final transient ResourceBundle defaultBundle;
    private final transient Essentials ess;
    private transient Locale currentLocale = defaultLocale;
    private final transient Map<Locale, ResourceBundle> loadedBundles = new ConcurrentHashMap<>();
    private final transient List<Locale> loadingBundles = new ArrayList<>();
    private transient ResourceBundle localeBundle;
    private final transient Map<Locale, Map<String, MessageFormat>> messageFormatCache = new ConcurrentHashMap<>();

    public I18n(final Essentials ess) {
        this.ess = ess;
        defaultBundle = ResourceBundle.getBundle(MESSAGES, Locale.ENGLISH, I18n.class.getClassLoader(), new UTF8PropertiesControl());
        localeBundle = defaultBundle;
    }

    public static String tlLiteral(final String tlKey, final Object... objects) {
        if (instance == null) {
            return "";
        }
        return tlLocale(instance.currentLocale, tlKey, objects);
    }

    public static String tlLocale(final Locale locale, final String tlKey, final Object... objects) {
        if (instance == null) {
            return "";
        }
        if (objects.length == 0) {
            return NODOUBLEMARK.matcher(instance.translate(locale, tlKey)).replaceAll("'");
        } else {
            return instance.format(locale, tlKey, objects);
        }
    }

    public static boolean hasKey(final String tlKey) {
        if (instance == null) {
            return false;
        }
        try {
            instance.defaultBundle.getString(tlKey);
            return true;
        } catch (final MissingResourceException ex) {
            return false;
        }
    }

    public static String capitalCase(final String input) {
        return input == null || input.isEmpty() ? input : input.toUpperCase(Locale.ENGLISH).charAt(0) + input.toLowerCase(Locale.ENGLISH).substring(1);
    }

    public void onEnable() {
        instance = this;
    }

    public void onDisable() {
        instance = null;
        shutdownExecutor();
    }

    private static synchronized ExecutorService getExecutor() {
        if (BUNDLE_LOADER_EXECUTOR == null || BUNDLE_LOADER_EXECUTOR.isShutdown() || BUNDLE_LOADER_EXECUTOR.isTerminated()) {
            BUNDLE_LOADER_EXECUTOR = Executors.newFixedThreadPool(2);
        }
        return BUNDLE_LOADER_EXECUTOR;
    }

    private static synchronized void shutdownExecutor() {
        final ExecutorService exec = BUNDLE_LOADER_EXECUTOR;
        if (exec == null) {
            return;
        }
        exec.shutdown();
        try {
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (final InterruptedException ignored) {
            exec.shutdownNow();
        }
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    private ResourceBundle getBundle(final Locale locale) {
        if (loadedBundles.containsKey(locale)) {
            return loadedBundles.get(locale);
        } else {
            synchronized (loadingBundles) {
                if (!loadingBundles.contains(locale)) {
                    loadingBundles.add(locale);
                    getExecutor().submit(() -> {
                        blockingLoadBundle(locale);
                        synchronized (loadingBundles) {
                            loadingBundles.remove(locale);
                        }
                    });
                }
            }
            return defaultBundle;
        }
    }

    public void blockingLoadBundle(final Locale locale) {
        if (!loadedBundles.containsKey(locale)) {
            ResourceBundle bundle;
            try {
                bundle = ResourceBundle.getBundle(MESSAGES, locale, new FileResClassLoader(I18n.class.getClassLoader(), ess), new UTF8PropertiesControl());
            } catch (final MissingResourceException ex) {
                try {
                    bundle = ResourceBundle.getBundle(MESSAGES, locale, I18n.class.getClassLoader(), new UTF8PropertiesControl());
                } catch (final MissingResourceException ex2) {
                    bundle = NULL_BUNDLE;
                }
            }
            loadedBundles.put(locale, bundle);
        }
    }

    private String translate(final Locale locale, final String string) {
        try {
            try {
                return getBundle(locale).getString(string);
            } catch (final MissingResourceException ex) {
                return localeBundle.getString(string);
            }
        } catch (final MissingResourceException ex) {
            if (ess != null && ess.getSettings() != null && ess.getSettings().isDebug()) {
                ess.getLogger().warn("Missing translation key \"{}\" in translation file {}", ex.getKey(), localeBundle.getLocale().toString());
            }
            return defaultBundle.getString(string);
        }
    }

    private String format(final Locale locale, final String string, final Object... objects) {
        String format = translate(locale, string);
        MessageFormat messageFormat = messageFormatCache.computeIfAbsent(locale, l -> new ConcurrentHashMap<>()).get(format);
        if (messageFormat == null) {
            try {
                messageFormat = new MessageFormat(format);
            } catch (final IllegalArgumentException e) {
                ess.getLogger().error("Invalid Translation key for '" + string + "': " + e.getMessage());
                format = format.replaceAll("\\{(\\D*?)}", "\\[$1\\]");
                messageFormat = new MessageFormat(format);
            }
            messageFormatCache.get(locale).put(format, messageFormat);
        }
        final Object[] processedArgs = mutateArgs(objects, arg -> {
            if (arg instanceof Text.ParsedPlaceholder) {
                return arg.toString();
            }
            return Text.get().legacyToMini(Text.get().escapeTags(arg.toString()));
        });
        synchronized (messageFormat) {
            return messageFormat.format(processedArgs).replace(' ', ' ');
        }
    }

    public static Object[] mutateArgs(final Object[] objects, final Function<Object, String> mutator) {
        final Object[] args = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            final Object object = objects[i];
            if (object instanceof Number || object instanceof java.util.Date || object == null) {
                args[i] = object;
                continue;
            }
            args[i] = mutator.apply(object);
        }
        return args;
    }

    public void updateLocale(final String loc) {
        if (loc != null && !loc.isEmpty()) {
            currentLocale = getLocale(loc);
        }
        ResourceBundle.clearCache();
        loadedBundles.clear();
        messageFormatCache.clear();
        ess.getLogger().info("Using locale {}", currentLocale.toString());
        try {
            localeBundle = ResourceBundle.getBundle(MESSAGES, currentLocale, new FileResClassLoader(I18n.class.getClassLoader(), ess), new UTF8PropertiesControl());
        } catch (final MissingResourceException ex) {
            try {
                localeBundle = ResourceBundle.getBundle(MESSAGES, currentLocale, I18n.class.getClassLoader(), new UTF8PropertiesControl());
            } catch (final MissingResourceException ex2) {
                localeBundle = NULL_BUNDLE;
            }
        }
    }

    public static Locale getLocale(final String loc) {
        if (loc == null || loc.isEmpty()) {
            return instance == null ? Locale.getDefault() : instance.currentLocale;
        }
        final String[] parts = loc.split("[_.]");
        if (parts.length == 1) {
            return Locale.of(parts[0]);
        }
        if (parts.length == 2) {
            return Locale.of(parts[0], parts[1]);
        }
        if (parts.length == 3) {
            return Locale.of(parts[0], parts[1], parts[2]);
        }
        return instance == null ? Locale.getDefault() : instance.currentLocale;
    }

    /**
     * Loads bundle overrides from {@code config/essentials-fabric/messages/}.
     */
    private static class FileResClassLoader extends ClassLoader {
        private final transient File messagesFolder;

        FileResClassLoader(final ClassLoader classLoader, final Essentials ess) {
            super(classLoader);
            this.messagesFolder = ess.getDataFolder().resolve("messages").toFile();
            this.messagesFolder.mkdirs();
        }

        @Override
        public URL getResource(final String string) {
            final File file = new File(messagesFolder, string);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (final MalformedURLException ignored) {
                }
            }
            return null;
        }

        @Override
        public InputStream getResourceAsStream(final String string) {
            final File file = new File(messagesFolder, string);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (final FileNotFoundException ignored) {
                }
            }
            return null;
        }
    }

    private static final class UTF8PropertiesControl extends ResourceBundle.Control {
        public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IOException {
            final String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                final URL url = loader.getResource(resourceName);
                if (url != null) {
                    final URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }

        @Override
        public Locale getFallbackLocale(final String baseName, final Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return null;
        }
    }
}
