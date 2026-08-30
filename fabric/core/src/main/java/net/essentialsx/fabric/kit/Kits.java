package net.essentialsx.fabric.kit;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.essentialsx.fabric.text.I18n.capitalCase;

/**
 * Kit definitions from {@code kits.yml} and {@code kits/*.yml}.
 */
public class Kits {
    private final Essentials ess;
    private final YamlFile rootConfig;
    private final Map<String, YamlFile> kitToConfigMap = new HashMap<>();
    private final Map<String, Map<String, Object>> kitMap = new HashMap<>();

    public Kits(final Essentials essentials) {
        this.ess = essentials;
        this.rootConfig = new YamlFile(essentials.getDataFolder().resolve("kits.yml"), "/kits.yml");
        reloadConfig();
    }

    public void reloadConfig() {
        rootConfig.load();
        parseKits();
    }

    public Path getFile() {
        return rootConfig.getFile();
    }

    private void parseKit(final String kitName, final Object kitSection, final YamlFile parentConfig) {
        if (kitSection instanceof Map) {
            final String effectiveKitName = kitName.toLowerCase(Locale.ENGLISH);
            kitToConfigMap.put(effectiveKitName, parentConfig);
            //noinspection unchecked
            kitMap.put(effectiveKitName, (Map<String, Object>) kitSection);
        }
    }

    private void parseKits() {
        kitToConfigMap.clear();
        kitMap.clear();
        final Map<String, Object> fileKits = rootConfig.getSection("kits");
        if (fileKits != null) {
            for (final Map.Entry<String, Object> kitEntry : fileKits.entrySet()) {
                parseKit(kitEntry.getKey(), kitEntry.getValue(), rootConfig);
            }
        }
        final Path kitsFolder = ess.getDataFolder().resolve("kits");
        if (!Files.isDirectory(kitsFolder)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(kitsFolder, "*.yml")) {
            for (final Path kitFile : stream) {
                final YamlFile kitConfig = new YamlFile(kitFile);
                kitConfig.load();
                final Map<String, Object> kits = kitConfig.getSection("kits");
                if (kits != null) {
                    for (final Map.Entry<String, Object> kitEntry : kits.entrySet()) {
                        parseKit(kitEntry.getKey(), kitEntry.getValue(), kitConfig);
                    }
                }
            }
        } catch (final IOException ignored) {
        }
    }

    public YamlFile getRootConfig() {
        return rootConfig;
    }

    public Set<String> getKitKeys() {
        return kitMap.keySet();
    }

    public Map<String, Object> getKit(final String name) {
        if (name != null) {
            return kitMap.get(name.replace('.', '_').replace('/', '_').toLowerCase(Locale.ENGLISH));
        }
        return null;
    }

    public String matchKit(final String name) {
        if (name != null) {
            for (final String kitName : kitMap.keySet()) {
                if (kitName.equalsIgnoreCase(name)) {
                    return kitName;
                }
            }
        }
        return null;
    }

    public void addKit(String name, final List<String> lines, final long delay) {
        name = name.replace('.', '_').replace('/', '_').toLowerCase(Locale.ENGLISH);
        rootConfig.setProperty("kits." + name + ".delay", delay);
        rootConfig.setProperty("kits." + name + ".items", lines);
        parseKits();
        rootConfig.save();
    }

    public void removeKit(String name) {
        name = name.replace('.', '_').replace('/', '_').toLowerCase(Locale.ENGLISH);
        if (!kitToConfigMap.containsKey(name) || !kitMap.containsKey(name)) {
            return;
        }
        final YamlFile config = kitToConfigMap.get(name);
        config.removeProperty("kits." + name);
        config.blockingSave();
        parseKits();
    }

    public String listKits(final Essentials ess, final User user) throws Exception {
        try {
            final StringBuilder list = new StringBuilder();
            for (final String kitItem : kitMap.keySet()) {
                if (user == null) {
                    list.append(" ").append(capitalCase(kitItem));
                } else if (user.isAuthorized("essentials.kits." + kitItem.toLowerCase(Locale.ENGLISH))) {
                    String cost = "";
                    String name = capitalCase(kitItem);
                    final BigDecimal costPrice = new Trade("kit-" + kitItem.toLowerCase(Locale.ENGLISH), ess).getCommandCost(user);
                    if (costPrice.signum() > 0) {
                        cost = user.playerTl("kitCost", NumberUtil.displayCurrency(costPrice, ess));
                    }
                    final Kit kit = new Kit(kitItem, ess);
                    final double nextUse = kit.getNextUse(user);
                    if (nextUse == -1 && ess.getSettings().isSkippingUsedOneTimeKitsFromKitList()) {
                        continue;
                    } else if (nextUse != 0) {
                        name = user.playerTl("kitDelay", name);
                    }
                    list.append(" ").append(name).append(cost);
                }
            }
            return list.toString().trim();
        } catch (final Exception ex) {
            throw new TranslatableException(ex, "kitError");
        }
    }
}
