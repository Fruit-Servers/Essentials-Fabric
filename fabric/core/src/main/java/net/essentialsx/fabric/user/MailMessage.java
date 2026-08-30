package net.essentialsx.fabric.user;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent mail entry (Section 6.2: sender UUID + name snapshot, sent/read/expiry).
 */
public final class MailMessage {
    private final boolean read;
    private final boolean legacy;
    private final String senderName;
    private final UUID senderId;
    private final long timestamp;
    private final long expire;
    private final String message;

    public MailMessage(final boolean read, final boolean legacy, final String sender, final UUID uuid, final long timestamp, final long expire, final String message) {
        this.read = read;
        this.legacy = legacy;
        this.senderName = sender;
        this.senderId = uuid;
        this.timestamp = timestamp;
        this.expire = expire;
        this.message = message;
    }

    public static MailMessage fromMap(final Map<String, Object> map) {
        final Object sender = map.get("sender-name");
        final Object uuid = map.get("sender-uuid");
        UUID senderId = null;
        if (uuid != null) {
            try {
                senderId = UUID.fromString(uuid.toString());
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return new MailMessage(
            bool(map.get("read")),
            bool(map.get("legacy")),
            sender == null ? null : sender.toString(),
            senderId,
            num(map.get("timestamp")),
            num(map.get("expire")),
            String.valueOf(map.getOrDefault("message", ""))
        );
    }

    public static MailMessage fromLegacy(final String message) {
        return new MailMessage(false, true, null, null, 0L, 0L, message);
    }

    private static boolean bool(final Object o) {
        return o instanceof Boolean ? (Boolean) o : o != null && Boolean.parseBoolean(o.toString());
    }

    private static long num(final Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong((String) o);
            } catch (final NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("read", read);
        map.put("legacy", legacy);
        if (senderName != null) {
            map.put("sender-name", senderName);
        }
        if (senderId != null) {
            map.put("sender-uuid", senderId.toString());
        }
        map.put("timestamp", timestamp);
        map.put("expire", expire);
        map.put("message", message);
        return map;
    }

    public MailMessage asRead() {
        return new MailMessage(true, legacy, senderName, senderId, timestamp, expire, message);
    }

    public boolean isRead() {
        return read;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String getSenderUsername() {
        return senderName;
    }

    public UUID getSenderUUID() {
        return senderId;
    }

    public long getTimeSent() {
        return timestamp;
    }

    public long getTimeExpire() {
        return expire;
    }

    public String getMessage() {
        return message;
    }

    public boolean isExpired() {
        if (getTimeExpire() == 0L) {
            return false;
        }
        return System.currentTimeMillis() >= getTimeExpire();
    }
}
