package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class NyanCommand extends EssentialsTreeNode {
    private static final String NYAN_TUNE = "1D#,1E,2F#,,2A#,1E,1D#,1E,2F#,2B,2D#,2E,2D#,2A#,2B,,2F#,,1D#,1E,2F#,2B,2C#,2A#,2B,2C#,2E,2D#,2E,2C#,,2F#,,2G#,,1D,1D#,,1C#,1D,1C#,1B,,1B,,1C#,,1D,,1D,1C#,1B,1C#,1D#,2F#,2G#,1D#,2F#,1C#,1D#,1B,1C#,1B,1D#,,2F#,,2G#,1D#,2F#,1C#,1D#,1B,1D,1D#,1D,1C#,1B,1C#,1D,,1B,1C#,1D#,2F#,1C#,1D,1C#,1B,1C#,,1B,,1C#,,2F#,,2G#,,1D,1D#,,1C#,1D,1C#,1B,,1B,,1C#,,1D,,1D,1C#,1B,1C#,1D#,2F#,2G#,1D#,2F#,1C#,1D#,1B,1C#,1B,1D#,,2F#,,2G#,1D#,2F#,1C#,1D#,1B,1D,1D#,1D,1C#,1B,1C#,1D,,1B,1C#,1D#,2F#,1C#,1D,1C#,1B,1C#,,1B,,1B,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1B,,";
    private transient TuneRunnable currentTune = null;

    public NyanCommand() {
        super(new String[] {"nyan", "nya"}, true);
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (currentTune != null) {
            currentTune.cancel();
        }
        currentTune = new TuneRunnable(ess, NYAN_TUNE, SoundEvents.NOTE_BLOCK_HARP.value(), ess::getOnlinePlayers);
        currentTune.start(20, 2);
    }

    private static class TuneRunnable implements Runnable {
        private static final Map<String, Float> NOTE_MAP = Map.ofEntries(
            Map.entry("1F#", 0.5f), Map.entry("1G", 0.53f), Map.entry("1G#", 0.56f), Map.entry("1A", 0.6f), Map.entry("1A#", 0.63f), Map.entry("1B", 0.67f),
            Map.entry("1C", 0.7f), Map.entry("1C#", 0.76f), Map.entry("1D", 0.8f), Map.entry("1D#", 0.84f), Map.entry("1E", 0.9f), Map.entry("1F", 0.94f),
            Map.entry("2F#", 1.0f), Map.entry("2G", 1.06f), Map.entry("2G#", 1.12f), Map.entry("2A", 1.18f), Map.entry("2A#", 1.26f), Map.entry("2B", 1.34f),
            Map.entry("2C", 1.42f), Map.entry("2C#", 1.5f), Map.entry("2D", 1.6f), Map.entry("2D#", 1.68f), Map.entry("2E", 1.78f), Map.entry("2F", 1.88f));
        private final Essentials ess;
        private final String[] tune;
        private final SoundEvent sound;
        private final Supplier<Collection<ServerPlayer>> players;
        private int i = 0;
        private int period = 2;
        private volatile boolean cancelled = false;

        TuneRunnable(final Essentials ess, final String tuneStr, final SoundEvent sound, final Supplier<Collection<ServerPlayer>> players) {
            this.ess = ess;
            this.tune = tuneStr.split(",");
            this.sound = sound;
            this.players = players;
        }

        void start(final int delay, final int period) {
            this.period = period;
            ess.scheduleSyncDelayedTask(this, delay);
        }

        void cancel() {
            cancelled = true;
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }
            final String note = tune[i];
            i++;
            if (i >= tune.length) {
                cancel();
            } else {
                ess.scheduleSyncDelayedTask(this, period);
            }
            if (note == null || note.isEmpty()) {
                return;
            }
            final Float pitch = NOTE_MAP.get(note);
            if (pitch == null) {
                return;
            }
            for (final ServerPlayer onlinePlayer : players.get()) {
                onlinePlayer.playNotifySound(sound, SoundSource.RECORDS, 1, pitch);
            }
        }
    }
}
