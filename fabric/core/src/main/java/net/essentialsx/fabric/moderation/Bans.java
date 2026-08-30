package net.essentialsx.fabric.moderation;

import com.mojang.authlib.GameProfile;
import net.essentialsx.fabric.Essentials;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Vanilla ban-list adapter (Section 13.1). Timed bans store an expiry that vanilla honours.
 */
public final class Bans {
    private Bans() {
    }

    public static GameProfile profile(final Essentials ess, final UUID uuid, final String name) {
        if (uuid != null) {
            return new GameProfile(uuid, name);
        }
        if (ess.getServer().getProfileCache() != null) {
            final Optional<GameProfile> cached = ess.getServer().getProfileCache().get(name);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        final UUID known = ess.getUsers().getUuid(name);
        if (known != null) {
            return new GameProfile(known, name);
        }
        return new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name);
    }

    public static void banPlayer(final Essentials ess, final UUID uuid, final String name, final String reason, final Date expires, final String source) {
        final UserBanList bans = ess.getServer().getPlayerList().getBans();
        bans.add(new UserBanListEntry(profile(ess, uuid, name), new Date(), source, expires, reason));
    }

    public static boolean unbanPlayer(final Essentials ess, final UUID uuid, final String name) {
        final UserBanList bans = ess.getServer().getPlayerList().getBans();
        final GameProfile profile = profile(ess, uuid, name);
        if (!bans.isBanned(profile)) {
            return false;
        }
        bans.remove(profile);
        return true;
    }

    public static boolean isBanned(final Essentials ess, final UUID uuid, final String name) {
        return ess.getServer().getPlayerList().getBans().isBanned(profile(ess, uuid, name));
    }

    public static UserBanListEntry getBan(final Essentials ess, final UUID uuid, final String name) {
        return ess.getServer().getPlayerList().getBans().get(profile(ess, uuid, name));
    }

    public static void banIp(final Essentials ess, final String ip, final String reason, final Date expires, final String source) {
        final IpBanList bans = ess.getServer().getPlayerList().getIpBans();
        bans.add(new IpBanListEntry(ip, new Date(), source, expires, reason));
    }

    public static boolean unbanIp(final Essentials ess, final String ip) {
        final IpBanList bans = ess.getServer().getPlayerList().getIpBans();
        if (!bans.isBanned(ip)) {
            return false;
        }
        bans.remove(ip);
        return true;
    }

    public static boolean isIpBanned(final Essentials ess, final String ip) {
        return ess.getServer().getPlayerList().getIpBans().isBanned(ip);
    }

    public static IpBanListEntry getIpBan(final Essentials ess, final String ip) {
        return ess.getServer().getPlayerList().getIpBans().get(ip);
    }
}
