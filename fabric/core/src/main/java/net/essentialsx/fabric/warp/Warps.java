package net.essentialsx.fabric.warp;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.WarpNotFoundException;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.utils.StringUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * One atomic file per warp under {@code warps/} (Section 9.2).
 */
public class Warps {
    private final Essentials ess;
    private final Map<StringIgnoreCase, YamlFile> warpPoints = new HashMap<>();
    private final Path warpsFolder;

    public Warps(final Essentials ess, final Path dataFolder) {
        this.ess = ess;
        warpsFolder = dataFolder.resolve("warps");
        try {
            Files.createDirectories(warpsFolder);
        } catch (final IOException ignored) {
        }
        reloadConfig();
    }

    public boolean isEmpty() {
        return warpPoints.isEmpty();
    }

    public boolean isWarp(final String name) {
        return warpPoints.containsKey(new StringIgnoreCase(name));
    }

    public Collection<String> getList() {
        final List<String> keys = new ArrayList<>();
        for (final StringIgnoreCase stringIgnoreCase : warpPoints.keySet()) {
            keys.add(stringIgnoreCase.getString());
        }
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        return keys;
    }

    public LazyLocation getWarp(final String warp) throws WarpNotFoundException {
        final YamlFile conf = warpPoints.get(new StringIgnoreCase(warp));
        if (conf == null) {
            throw new WarpNotFoundException();
        }
        final LazyLocation loc = conf.getLocation(null);
        if (loc == null) {
            throw new WarpNotFoundException();
        }
        return loc;
    }

    /** Warp location without world availability check (for warpinfo). */
    public LazyLocation getWarpRaw(final String warp) {
        final YamlFile conf = warpPoints.get(new StringIgnoreCase(warp));
        return conf == null ? null : conf.getLocation(null);
    }

    public void setWarp(final String name, final LazyLocation loc) throws Exception {
        setWarp(null, name, loc);
    }

    public void setWarp(final IUser user, final String name, final LazyLocation loc) throws Exception {
        final String filename = StringUtil.sanitizeFileName(name);
        YamlFile conf = warpPoints.get(new StringIgnoreCase(name));
        if (conf == null) {
            final Path confFile = warpsFolder.resolve(filename + ".yml");
            if (Files.exists(confFile)) {
                throw new TranslatableException("similarWarpExist");
            }
            conf = new YamlFile(confFile);
            conf.load();
            warpPoints.put(new StringIgnoreCase(name), conf);
        }
        conf.setProperty(null, loc.toMap());
        conf.setProperty("name", name);
        if (user != null) {
            conf.setProperty("lastowner", user.getUUID().toString());
        }
        conf.save();
    }

    public UUID getLastOwner(final String warp) throws WarpNotFoundException {
        final YamlFile conf = warpPoints.get(new StringIgnoreCase(warp));
        if (conf == null) {
            throw new WarpNotFoundException();
        }
        return StringUtil.toUUID(conf.getString("lastowner", ""));
    }

    public void removeWarp(final String name) throws Exception {
        final YamlFile conf = warpPoints.get(new StringIgnoreCase(name));
        if (conf == null) {
            throw new TranslatableException("warpNotExist");
        }
        try {
            Files.deleteIfExists(conf.getFile());
        } catch (final IOException e) {
            throw new TranslatableException("warpDeleteError");
        }
        warpPoints.remove(new StringIgnoreCase(name));
    }

    public final void reloadConfig() {
        warpPoints.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(warpsFolder, "*.yml")) {
            for (final Path file : stream) {
                final String filename = file.getFileName().toString();
                try {
                    final YamlFile conf = new YamlFile(file);
                    conf.load();
                    final String name = conf.getString("name", null);
                    if (name != null && conf.hasProperty("world")) {
                        warpPoints.put(new StringIgnoreCase(name), conf);
                    }
                } catch (final Exception ex) {
                    ess.getLogger().warn(Text.get().miniToLegacy(tlLiteral("loadWarpError", filename)), ex);
                }
            }
        } catch (final IOException ignored) {
        }
    }

    public int getCount() {
        return getList().size();
    }

    private static class StringIgnoreCase {
        private final String string;

        StringIgnoreCase(final String string) {
            this.string = string;
        }

        @Override
        public int hashCode() {
            return getString().toLowerCase(Locale.ENGLISH).hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof StringIgnoreCase) {
                return getString().equalsIgnoreCase(((StringIgnoreCase) o).getString());
            }
            return false;
        }

        public String getString() {
            return string;
        }
    }
}
