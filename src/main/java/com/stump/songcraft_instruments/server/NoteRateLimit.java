package com.stump.songcraft_instruments.server;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.recording.Recording;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Caps how many notes a player may play at once and over a second, so that nobody can flood the players around
 * them (or a record, and everyone around its looper) with notes. Notes past the cap are dropped: not played to
 * anyone else, and not recorded. Imported records are held to the same caps, per part.
 * <p>
 * Only new notes count. Dampens and held note releases always go through, so that no note is left sounding.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class NoteRateLimit {
    /** The most notes played in a single tick; enough for a 10 finger chord or a full strum */
    public static final int MAX_NOTES_PER_TICK = 16;
    /** The most notes played over any {@link #WINDOW_TICKS} ticks */
    public static final int MAX_NOTES_PER_SECOND = 60;
    private static final int WINDOW_TICKS = 20;

    private static final Map<UUID, Window> PLAYER_WINDOWS = new HashMap<>();

    /**
     * The ticks of the notes counted over the last {@link #WINDOW_TICKS} ticks
     */
    private static final class Window {
        private final ArrayDeque<Integer> noteTicks = new ArrayDeque<>();

        /**
         * Counts a note at the given tick, unless it would go over a cap. Ticks must not go backwards.
         * @return Whether the note is within the caps
         */
        boolean tryCount(final int tick) {
            while (!noteTicks.isEmpty() && noteTicks.peekFirst() <= tick - WINDOW_TICKS)
                noteTicks.removeFirst();

            if (noteTicks.size() >= MAX_NOTES_PER_SECOND)
                return false;

            int notesThisTick = 0;
            for (var it = noteTicks.descendingIterator(); it.hasNext() && it.next() == tick; )
                notesThisTick++;
            if (notesThisTick >= MAX_NOTES_PER_TICK)
                return false;

            noteTicks.addLast(tick);
            return true;
        }
    }

    private NoteRateLimit() {}

    /**
     * Counts a new note played by the player. Server only.
     * @return Whether the note may be played; false if the player is playing too many notes
     */
    public static boolean tryPlay(final ServerPlayer player) {
        return PLAYER_WINDOWS.computeIfAbsent(player.getUUID(), (id) -> new Window())
            .tryCount(player.server.getTickCount());
    }

    /**
     * @return Whether any part of the recording plays more notes than a player could play live
     */
    public static boolean isExceededBy(final Recording recording) {
        final Window[] windows = new Window[recording.performers().size()];

        // Notes are sorted by timestamp, so each part's ticks go forwards
        for (int i = 0; i < recording.noteCount(); i++) {
            final int[] note = recording.note(i);
            if (!isNewNote(note))
                continue;

            final int performer = note[Recording.PERFORMER];
            if (windows[performer] == null)
                windows[performer] = new Window();
            if (!windows[performer].tryCount(note[Recording.TIMESTAMP]))
                return true;
        }

        return false;
    }

    private static boolean isNewNote(final int[] note) {
        return switch (note[Recording.TYPE]) {
            case Recording.TYPE_REGULAR -> true;
            case Recording.TYPE_HELD -> note[Recording.HELD_PHASE] == Recording.PHASE_ATTACK;
            default -> false;
        };
    }

    @SubscribeEvent
    public static void onPlayerLeave(final PlayerLoggedOutEvent event) {
        PLAYER_WINDOWS.remove(event.getEntity().getUUID());
    }
}
