package com.stump.songcraft_instruments.recording;

import com.stump.songcraft_instruments.recording.Recording.Performer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Reads and writes the {@code .screc} recording file format, used both in the world save and for exported records.
 * <p>
 * The file is gzipped. Inside, strings and particle colors are stored once in palettes, and each note stores
 * the ticks since the previous note and only the fields that differ from its performer's previous note.
 * A note usually takes 3 to 6 bytes before compression.
 *
 * <pre>
 * "SCRC", varint version
 * varint length + 1 (0 for an unfinished recording)
 * instrument ID palette, sound type palette: varint count, UTF strings
 * performers: varint count, then UUID (2 longs), UTF name, varint color count, int colors
 * particle color palette: varint count, ints
 * varint note count, then per note:
 *   varint ticks since the previous note
 *   byte flags: bits 0-1 type, bit 2 held release, bit 3 same performer as the previous note,
 *               bits 4-7 same sound / pitch / volume / color as the performer's previous note
 *   [varint performer] [varint instrument, varint sound type] varint sound index [zigzag pitch] [zigzag volume] [varint color]
 *   (dampens end after the performer)
 * </pre>
 */
public final class RecordingCodec {
    private static final byte[] MAGIC = {'S', 'C', 'R', 'C'};
    public static final int VERSION = 1;

    /**
     * Notes may not be later than a day in. Bounds the playback index of imported files.
     */
    public static final int MAX_TICK = 20 * 60 * 60 * 24;
    /**
     * The most a note takes encoded, plus room for the palettes. Bounds how far a file with a note limit may decompress.
     */
    private static final long MAX_BYTES_PER_NOTE = 40, MAX_HEADER_BYTES = 1024 * 1024;
    private static final int MAX_STRING_PALETTE = 4096, MAX_PERFORMERS = 1024, MAX_PERFORMER_COLORS = 64,
        MAX_COLORS = 65536;

    private static final int
        FLAG_TYPE_MASK = 0b11,
        FLAG_RELEASE = 1 << 2,
        FLAG_SAME_PERFORMER = 1 << 3,
        FLAG_SAME_SOUND = 1 << 4,
        FLAG_SAME_PITCH = 1 << 5,
        FLAG_SAME_VOLUME = 1 << 6,
        FLAG_SAME_COLOR = 1 << 7;

    private RecordingCodec() {}


    public static class InvalidRecordingException extends IOException {
        public InvalidRecordingException(final String message) {
            super(message);
        }
    }

    /**
     * Thrown before reading the notes of a recording that has more than allowed
     */
    public static class TooManyNotesException extends InvalidRecordingException {
        public final int noteCount;

        public TooManyNotesException(final int noteCount) {
            super("Too many notes: " + noteCount);
            this.noteCount = noteCount;
        }
    }


    //#region Writing

    /**
     * @return The recording as an uncompressed byte array, from which its {@link #id} is derived
     */
    public static byte[] encodeRaw(final Recording recording) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final DataOutputStream out = new DataOutputStream(bytes)) {
            write(recording, out);
        } catch (IOException e) {
            // Not thrown by in-memory streams
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /**
     * @return The recording as the contents of a {@code .screc} file
     */
    public static byte[] encode(final Recording recording) {
        return compress(encodeRaw(recording));
    }

    public static byte[] compress(final byte[] raw) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(raw);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void write(final Recording recording, final DataOutputStream out) throws IOException {
        out.write(MAGIC);
        writeVarInt(out, VERSION);
        writeVarInt(out, recording.length() + 1);

        writeStrings(out, recording.instrumentIds());
        writeStrings(out, recording.soundTypes());

        writeVarInt(out, recording.performers().size());
        for (final Performer performer : recording.performers()) {
            out.writeLong(performer.id().getMostSignificantBits());
            out.writeLong(performer.id().getLeastSignificantBits());
            out.writeUTF(performer.name());
            writeVarInt(out, performer.colors().length);
            for (final int color : performer.colors())
                out.writeInt(color);
        }

        // Particle colors are per note, but come from a few color sets
        final List<Integer> colors = new ArrayList<>();
        final Map<Integer, Integer> colorIndices = new HashMap<>();
        for (int i = 0; i < recording.noteCount(); i++) {
            final int[] note = recording.note(i);
            if (note[Recording.TYPE] != Recording.TYPE_DAMPEN && !colorIndices.containsKey(note[Recording.PARTICLE_COLOR])) {
                colorIndices.put(note[Recording.PARTICLE_COLOR], colors.size());
                colors.add(note[Recording.PARTICLE_COLOR]);
            }
        }
        writeVarInt(out, colors.size());
        for (final int color : colors)
            out.writeInt(color);

        writeVarInt(out, recording.noteCount());
        final Map<Integer, int[]> previousByPerformer = new HashMap<>();
        int previousTick = 0, previousPerformer = -1;

        for (int i = 0; i < recording.noteCount(); i++) {
            final int[] note = recording.note(i);
            final int type = note[Recording.TYPE], performer = note[Recording.PERFORMER];
            final int[] previous = previousByPerformer.get(performer);

            int flags = type;
            if (performer == previousPerformer)
                flags |= FLAG_SAME_PERFORMER;

            if (type != Recording.TYPE_DAMPEN) {
                if (type == Recording.TYPE_HELD && note[Recording.HELD_PHASE] == Recording.PHASE_RELEASE)
                    flags |= FLAG_RELEASE;
                if (previous != null) {
                    if (previous[Recording.INSTRUMENT_ID] == note[Recording.INSTRUMENT_ID]
                            && previous[Recording.SOUND_TYPE] == note[Recording.SOUND_TYPE])
                        flags |= FLAG_SAME_SOUND;
                    if (previous[Recording.PITCH] == note[Recording.PITCH])
                        flags |= FLAG_SAME_PITCH;
                    if (previous[Recording.VOLUME] == note[Recording.VOLUME])
                        flags |= FLAG_SAME_VOLUME;
                    if (previous[Recording.PARTICLE_COLOR] == note[Recording.PARTICLE_COLOR])
                        flags |= FLAG_SAME_COLOR;
                }
            }

            writeVarInt(out, note[Recording.TIMESTAMP] - previousTick);
            out.writeByte(flags);
            if ((flags & FLAG_SAME_PERFORMER) == 0)
                writeVarInt(out, performer);

            previousTick = note[Recording.TIMESTAMP];
            previousPerformer = performer;

            if (type == Recording.TYPE_DAMPEN)
                continue;

            if ((flags & FLAG_SAME_SOUND) == 0) {
                writeVarInt(out, note[Recording.INSTRUMENT_ID]);
                writeVarInt(out, note[Recording.SOUND_TYPE]);
            }
            writeVarInt(out, note[Recording.SOUND_INDEX]);
            if ((flags & FLAG_SAME_PITCH) == 0)
                writeVarInt(out, zigzag(note[Recording.PITCH]));
            if ((flags & FLAG_SAME_VOLUME) == 0)
                writeVarInt(out, zigzag(note[Recording.VOLUME]));
            if ((flags & FLAG_SAME_COLOR) == 0)
                writeVarInt(out, colorIndices.get(note[Recording.PARTICLE_COLOR]));

            previousByPerformer.put(performer, note);
        }
    }

    private static void writeStrings(final DataOutputStream out, final List<String> strings) throws IOException {
        writeVarInt(out, strings.size());
        for (final String string : strings)
            out.writeUTF(string);
    }

    private static void writeVarInt(final DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }
    private static int zigzag(final int value) {
        return (value << 1) ^ (value >> 31);
    }

    //#endregion


    //#region Reading

    /**
     * Reads and fully validates a {@code .screc} file of this world, so that it can be played back safely.
     * @throws InvalidRecordingException If the file is not a valid recording
     */
    public static Recording decode(final byte[] file) throws InvalidRecordingException {
        return decode(file, -1);
    }

    /**
     * Reads and fully validates a {@code .screc} file, so that a recording read from anywhere
     * (including other players' imports) can be played back safely.
     * @param maxNotes The most notes the recording may have, checked before they are read
     * (and bounding how much memory the file can take). Negative for no limit.
     * @throws TooManyNotesException If the recording has more than {@code maxNotes} notes
     * @throws InvalidRecordingException If the file is not a valid recording
     */
    public static Recording decode(final byte[] file, final int maxNotes) throws InvalidRecordingException {
        final long maxBytes = (maxNotes < 0) ? Long.MAX_VALUE : MAX_HEADER_BYTES + maxNotes * MAX_BYTES_PER_NOTE;

        try (final DataInputStream in = new DataInputStream(
                new LimitedInputStream(new GZIPInputStream(new ByteArrayInputStream(file)), maxBytes))) {
            return read(in, maxNotes);
        } catch (InvalidRecordingException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidRecordingException("Unreadable recording: " + e);
        }
    }

    private static Recording read(final DataInputStream in, final int maxNotes) throws IOException {
        final byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        for (int i = 0; i < MAGIC.length; i++) {
            if (magic[i] != MAGIC[i])
                throw new InvalidRecordingException("Not a recording file");
        }

        final int version = readVarInt(in);
        if (version != VERSION)
            throw new InvalidRecordingException("Unsupported recording version " + version);

        final int length = readVarInt(in) - 1;
        check(length >= -1 && length <= MAX_TICK, "length");

        final List<String> instrumentIds = readStrings(in), soundTypes = readStrings(in);

        final int performerCount = readVarInt(in);
        check(performerCount >= 0 && performerCount <= MAX_PERFORMERS, "performer count");
        final List<Performer> performers = new ArrayList<>(performerCount);
        final HashSet<UUID> performerIds = new HashSet<>();
        for (int i = 0; i < performerCount; i++) {
            final UUID id = new UUID(in.readLong(), in.readLong());
            check(performerIds.add(id), "duplicate performer");
            final String name = in.readUTF();
            final int colorCount = readVarInt(in);
            check(colorCount >= 0 && colorCount <= MAX_PERFORMER_COLORS, "performer color count");
            final int[] colors = new int[colorCount];
            for (int j = 0; j < colorCount; j++)
                colors[j] = in.readInt();
            performers.add(new Performer(id, name, colors));
        }

        final int colorCount = readVarInt(in);
        check(colorCount >= 0 && colorCount <= MAX_COLORS, "color count");
        final List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < colorCount; i++)
            colors.add(in.readInt());

        final int noteCount = readVarInt(in);
        check(noteCount >= 0, "note count");
        if (maxNotes >= 0 && noteCount > maxNotes)
            throw new TooManyNotesException(noteCount);
        final List<int[]> notes = new ArrayList<>();
        final Map<Integer, int[]> previousByPerformer = new HashMap<>();
        int tick = 0, previousPerformer = -1;

        for (int i = 0; i < noteCount; i++) {
            final int delta = readVarInt(in);
            check(delta >= 0 && delta <= MAX_TICK - tick, "note timestamp");
            tick += delta;

            final int flags = in.readUnsignedByte();
            final int type = flags & FLAG_TYPE_MASK;
            check(type <= Recording.TYPE_DAMPEN, "note type");

            final int performer = ((flags & FLAG_SAME_PERFORMER) != 0) ? previousPerformer : readVarInt(in);
            check(performer >= 0 && performer < performerCount, "note performer");
            previousPerformer = performer;

            if (type == Recording.TYPE_DAMPEN) {
                notes.add(new int[] {tick, type, performer});
                continue;
            }

            final int[] previous = previousByPerformer.get(performer);
            final boolean sameSound = (flags & FLAG_SAME_SOUND) != 0, samePitch = (flags & FLAG_SAME_PITCH) != 0,
                sameVolume = (flags & FLAG_SAME_VOLUME) != 0, sameColor = (flags & FLAG_SAME_COLOR) != 0;
            check(previous != null || !(sameSound || samePitch || sameVolume || sameColor), "note without a previous note");

            final int instrument = sameSound ? previous[Recording.INSTRUMENT_ID] : readVarInt(in);
            final int soundType = sameSound ? previous[Recording.SOUND_TYPE] : readVarInt(in);
            check(instrument >= 0 && instrument < instrumentIds.size(), "note instrument");
            check(soundType >= 0 && soundType < soundTypes.size(), "note sound type");

            final int soundIndex = readVarInt(in);
            final int pitch = samePitch ? previous[Recording.PITCH] : unzigzag(readVarInt(in));
            final int volume = sameVolume ? previous[Recording.VOLUME] : unzigzag(readVarInt(in));

            final int color;
            if (sameColor) {
                color = previous[Recording.PARTICLE_COLOR];
            } else {
                final int colorIndex = readVarInt(in);
                check(colorIndex >= 0 && colorIndex < colors.size(), "note color");
                color = colors.get(colorIndex);
            }

            final int[] note = (type == Recording.TYPE_HELD)
                ? new int[] {tick, type, performer, pitch, volume, color, instrument, soundType, soundIndex,
                    ((flags & FLAG_RELEASE) != 0) ? Recording.PHASE_RELEASE : Recording.PHASE_ATTACK}
                : new int[] {tick, type, performer, pitch, volume, color, instrument, soundType, soundIndex};
            notes.add(note);
            previousByPerformer.put(performer, note);
        }

        check(in.read() == -1, "trailing data");
        return new Recording(length, instrumentIds, soundTypes, performers, notes.toArray(int[][]::new));
    }

    private static List<String> readStrings(final DataInputStream in) throws IOException {
        final int count = readVarInt(in);
        check(count >= 0 && count <= MAX_STRING_PALETTE, "palette size");
        final List<String> strings = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            strings.add(in.readUTF());
        return strings;
    }

    private static int readVarInt(final DataInputStream in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            final int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return value;
        }
        throw new InvalidRecordingException("Malformed number");
    }
    private static int unzigzag(final int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    private static void check(final boolean condition, final String what) throws InvalidRecordingException {
        if (!condition)
            throw new InvalidRecordingException("Invalid " + what);
    }

    /**
     * Fails once more than the limit is read
     */
    private static final class LimitedInputStream extends FilterInputStream {
        private long remaining;

        LimitedInputStream(final InputStream in, final long limit) {
            super(in);
            remaining = limit;
        }

        @Override
        public int read() throws IOException {
            final int b = super.read();
            if (b != -1 && --remaining < 0)
                throw new InvalidRecordingException("Recording too large");
            return b;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            final int read = super.read(b, off, len);
            if (read > 0 && (remaining -= read) < 0)
                throw new InvalidRecordingException("Recording too large");
            return read;
        }
    }

    //#endregion


    /**
     * @return The ID of the recording: the first 128 bits of the SHA-256 of its uncompressed encoding, in hex.
     * Identical recordings share an ID, so copies and re-imports never store a recording twice.
     */
    public static String id(final byte[] raw) {
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            final StringBuilder id = new StringBuilder(32);
            for (int i = 0; i < 16; i++)
                id.append(String.format("%02x", hash[i]));
            return id.toString();
        } catch (NoSuchAlgorithmException e) {
            // Every JVM provides SHA-256
            throw new IllegalStateException(e);
        }
    }

    public static boolean isValidId(final String id) {
        return id != null && id.matches("[0-9a-f]{32}");
    }
}
