package net.essentialsx.fabric.teleport;

/**
 * Cause of an Essentials-initiated teleport (mirrors Bukkit's TeleportCause subset).
 */
public enum TeleportCause {
    COMMAND,
    PLUGIN,
    SPECTATE,
    UNKNOWN
}
