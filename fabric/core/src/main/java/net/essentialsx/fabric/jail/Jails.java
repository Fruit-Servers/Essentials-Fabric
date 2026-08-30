package net.essentialsx.fabric.jail;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.listener.JailListener;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.LazyLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Jail definitions stored in {@code jail.yml} (Section 9.3). Enforcement hooks live in
 * {@link net.essentialsx.fabric.listener.JailListener}.
 */
public class Jails {
    private final Essentials ess;
    private final YamlFile config;
    private final Map<String, LazyLocation> jails = new HashMap<>();

    public Jails(final Essentials ess) {
        this.ess = ess;
        this.config = new YamlFile(ess.getDataFolder().resolve("jail.yml"));
        reloadConfig();
    }

    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        synchronized (jails) {
            config.load();
            jails.clear();
            final Map<String, Object> jailsNode = config.getSection("jails");
            if (jailsNode != null) {
                for (final Map.Entry<String, Object> entry : jailsNode.entrySet()) {
                    if (!(entry.getValue() instanceof Map)) {
                        continue;
                    }
                    final LazyLocation loc = LazyLocation.fromMap((Map<String, Object>) entry.getValue());
                    if (loc != null) {
                        jails.put(entry.getKey().toLowerCase(Locale.ENGLISH), loc);
                    }
                }
            }
        }
    }

    public LazyLocation getJail(String jailName) throws Exception {
        if (jailName == null) {
            throw new TranslatableException("jailNotExist");
        }
        jailName = jailName.toLowerCase(Locale.ENGLISH);
        synchronized (jails) {
            if (!jails.containsKey(jailName)) {
                throw new TranslatableException("jailNotExist");
            }
            final LazyLocation location = jails.get(jailName);
            if (!location.isAvailable(ess.getServer())) {
                throw new TranslatableException("jailWorldNotExist");
            }
            return location;
        }
    }

    public Collection<String> getList() {
        synchronized (jails) {
            return new ArrayList<>(jails.keySet());
        }
    }

    public void removeJail(String jail) {
        if (jail == null) {
            return;
        }
        jail = jail.toLowerCase(Locale.ENGLISH);
        synchronized (jails) {
            if (jails.remove(jail) != null) {
                config.removeProperty("jails." + jail);
                config.save();
            }
        }
    }

    public void sendToJail(final IUser user, final String jailName, final CompletableFuture<Boolean> future) throws Exception {
        if (jailName == null || jailName.isEmpty()) {
            return;
        }
        final String jail = jailName.toLowerCase(Locale.ENGLISH);
        synchronized (jails) {
            if (jails.containsKey(jail)) {
                if (user.isOnline()) {
                    JailListener.allowTeleport(user.getUUID());
                    user.getAsyncTeleport().now(getJail(jail), false, TeleportCause.COMMAND, future);
                    future.thenAccept(success -> user.setJail(jail));
                    return;
                }
                user.setJail(jail);
            }
        }
    }

    public void setJail(String jailName, final LazyLocation loc) {
        jailName = jailName.toLowerCase(Locale.ENGLISH);
        synchronized (jails) {
            jails.put(jailName, loc);
            config.setProperty("jails." + jailName, loc.toMap());
            config.save();
        }
    }

    public int getCount() {
        return getList().size();
    }
}
