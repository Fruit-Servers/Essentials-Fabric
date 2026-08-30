package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player client time and weather, implemented the same way Bukkit does it: the server keeps a
 * per-player offset/override and re-sends the vanilla time/weather packets every 20 ticks so the
 * client displays the player-specific values instead of the world's.
 * <p>
 * Backs {@code /ptime} and {@code /pweather} (sections 6.9/6.10 of the design doc).
 */
public final class PlayerTimeWeather {
    public enum Weather { CLEAR, DOWNFALL }

    private record TimeState(long offset, boolean relative) {
    }

    private final Essentials ess;
    private final Map<UUID, TimeState> times = new ConcurrentHashMap<>();
    private final Map<UUID, Weather> weathers = new ConcurrentHashMap<>();

    public PlayerTimeWeather(final Essentials ess) {
        this.ess = ess;
    }

    // ------------------------------------------------------------------ time

    public boolean hasPlayerTime(final UUID uuid) {
        return times.containsKey(uuid);
    }

    public long getPlayerTimeOffset(final UUID uuid) {
        final TimeState state = times.get(uuid);
        return state == null ? 0 : state.offset();
    }

    public boolean isPlayerTimeRelative(final UUID uuid) {
        final TimeState state = times.get(uuid);
        return state == null || state.relative();
    }

    /** Player-visible day time (mirrors Bukkit {@code Player#getPlayerTime}). */
    public long getPlayerTime(final ServerPlayer player) {
        final TimeState state = times.get(player.getUUID());
        if (state == null) {
            return player.serverLevel().getDayTime();
        }
        if (state.relative()) {
            return player.serverLevel().getDayTime() + state.offset();
        }
        return state.offset();
    }

    /** Mirrors Bukkit {@code Player#setPlayerTime(long, boolean)}. */
    public void setPlayerTime(final ServerPlayer player, final long time, final boolean relative) {
        final long offset = relative ? time - player.serverLevel().getDayTime() : time;
        times.put(player.getUUID(), new TimeState(offset, relative));
        sendTime(player);
    }

    public void resetPlayerTime(final ServerPlayer player) {
        times.remove(player.getUUID());
        sendTime(player);
    }

    private void sendTime(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        final boolean cycle = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        final TimeState state = times.get(player.getUUID());
        long dayTime = level.getDayTime();
        boolean doCycle = cycle;
        if (state != null) {
            if (state.relative()) {
                dayTime = level.getDayTime() + state.offset();
            } else {
                dayTime = state.offset();
                doCycle = false;
            }
        }
        // Vanilla encodes "no daylight cycle" as a negative day time.
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), doCycle ? dayTime : -Math.max(0, dayTime) - 1, doCycle));
    }

    // --------------------------------------------------------------- weather

    public Weather getPlayerWeather(final UUID uuid) {
        return weathers.get(uuid);
    }

    public void setPlayerWeather(final ServerPlayer player, final Weather weather) {
        weathers.put(player.getUUID(), weather);
        sendWeather(player);
    }

    public void resetPlayerWeather(final ServerPlayer player) {
        weathers.remove(player.getUUID());
        sendWeather(player);
    }

    private void sendWeather(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        final Weather override = weathers.get(player.getUUID());
        final boolean raining = override == null ? level.isRaining() : override == Weather.DOWNFALL;
        if (raining) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, override == null ? level.getRainLevel(1.0F) : 1.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, override == null ? level.getThunderLevel(1.0F) : 0.0F));
        } else {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
        }
    }

    // ------------------------------------------------------------------ hooks

    /** Called every server tick; re-sends overrides on the same cadence as vanilla's time sync. */
    public void tick(final long serverTick) {
        if (serverTick % 20 != 0 || (times.isEmpty() && weathers.isEmpty())) {
            return;
        }
        for (final ServerPlayer player : ess.getOnlinePlayers()) {
            final UUID uuid = player.getUUID();
            if (times.containsKey(uuid)) {
                sendTime(player);
            }
            if (weathers.containsKey(uuid)) {
                sendWeather(player);
            }
        }
    }

    /** Vanilla resends world weather on dimension change, so overrides must be re-applied. */
    public void onChangeWorld(final ServerPlayer player) {
        if (times.containsKey(player.getUUID())) {
            sendTime(player);
        }
        if (weathers.containsKey(player.getUUID())) {
            sendWeather(player);
        }
    }

    public void remove(final UUID uuid) {
        times.remove(uuid);
        weathers.remove(uuid);
    }
}
