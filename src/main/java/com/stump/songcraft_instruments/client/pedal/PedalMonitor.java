package com.stump.songcraft_instruments.client.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.IHeldInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.networking.packet.S2CPedalMonitorPacket;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pedals the local player hears their own notes through.
 * <p>Speakers don't relay notes back to whoever played them, so the server tells the player the pedal chains of the
 * speakers their open instrument plays through (see {@code PedalMonitorSync}), and their own copy of each note is
 * played through one of them: the nearest speaker's, like a guitarist hearing the amp they stand by.</p>
 * <p>The monitored speaker only changes once another is clearly nearer ({@link #SWITCH_MARGIN}), so standing between
 * two speakers doesn't flip between them; and only between notes, as a note keeps the chain it started with.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class PedalMonitor {
    /**
     * How much nearer (in blocks) another speaker must be before it is monitored instead
     */
    private static final double SWITCH_MARGIN = 3;
    /**
     * How long (in ticks) a chain must stay the same before the open instrument's sounds are rendered through it,
     * so turning a pedal's knobs doesn't render every sound at every step
     */
    private static final int PREWARM_DELAY = 10;

    private static List<S2CPedalMonitorPacket.Entry> speakers = List.of();
    @Nullable
    private static BlockPos monitoredSpeaker;
    private static int prewarmIn = -1;

    private PedalMonitor() {}

    public static void setSpeakers(final List<S2CPedalMonitorPacket.Entry> entries) {
        speakers = List.copyOf(entries);
        if (find(monitoredSpeaker) == null)
            monitoredSpeaker = null;
        prewarmIn = speakers.isEmpty() ? -1 : PREWARM_DELAY;
    }

    /**
     * @return The pedals the local player's own notes should be played through right now
     */
    public static PedalChain chain() {
        final S2CPedalMonitorPacket.Entry entry = updateMonitored();
        return (entry == null) ? PedalChain.NONE : entry.chain();
    }

    @Nullable
    private static S2CPedalMonitorPacket.Entry updateMonitored() {
        if (speakers.isEmpty())
            return null;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return null;
        final Vec3 playerPos = minecraft.player.position();

        S2CPedalMonitorPacket.Entry nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (final S2CPedalMonitorPacket.Entry entry : speakers) {
            final double dist = entry.speakerPos().getCenter().distanceTo(playerPos);
            if (dist < nearestDist) {
                nearest = entry;
                nearestDist = dist;
            }
        }

        final S2CPedalMonitorPacket.Entry current = find(monitoredSpeaker);
        if (current == null || nearestDist + SWITCH_MARGIN < current.speakerPos().getCenter().distanceTo(playerPos)) {
            monitoredSpeaker = nearest.speakerPos();
            return nearest;
        }
        return current;
    }

    @Nullable
    private static S2CPedalMonitorPacket.Entry find(@Nullable final BlockPos speakerPos) {
        if (speakerPos == null)
            return null;
        for (final S2CPedalMonitorPacket.Entry entry : speakers)
            if (entry.speakerPos().equals(speakerPos))
                return entry;
        return null;
    }


    //#region Prewarming

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || prewarmIn < 0)
            return;
        if (--prewarmIn > 0)
            return;

        prewarmIn = -1;
        InstrumentScreen.getCurrentScreen().ifPresent(PedalMonitor::prewarm);
    }

    /**
     * Renders the open instrument's sounds through the monitored chain, so the player's first notes through it
     * aren't held up by rendering
     */
    private static void prewarm(final InstrumentScreen screen) {
        final PedalChain chain = chain();
        if (chain.isEmpty())
            return;

        // Each sound, at each pitch it's played at by the screen's notes (which only matters for chains with
        // effects that play out over time, see PedalSounds)
        final Set<Played> played = new HashSet<>();
        final HeldNoteSound[] heldSounds = (screen instanceof IHeldInstrumentScreen heldScreen)
            ? heldScreen.getHeldNoteSounds() : null;

        for (final NoteButton note : screen.notesIterable()) {
            final NoteSound sound = note.getSound();
            if (sound == null)
                continue;
            final float pitch = NoteSound.getPitchByNoteOffset(NoteSound.clampPitch(note.getPitch()));

            final HeldNoteSound held = heldOf(heldSounds, sound);
            if (held == null) {
                played.add(new Played(sound, pitch, chain));
                continue;
            }

            // Held notes only get effects that play out over time on their release
            played.add(new Played(held.attack(), pitch, chain.withoutTimeBased()));
            played.add(new Played(held.hold(), pitch, chain.withoutTimeBased()));
            if (held.release() != null)
                played.add(new Played(held.release(), pitch, chain));
        }

        final SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        final RandomSource random = SoundInstance.createUnseededRandom();
        for (final Played sound : played) {
            if (sound.chain().isEmpty())
                continue;
            prewarm(soundManager, random, sound.sound().getStereo(), sound.chain(), sound.pitch());
            prewarm(soundManager, random, sound.sound().getMono(), sound.chain(), sound.pitch());
        }
    }
    private record Played(NoteSound sound, float pitch, PedalChain chain) {}

    @Nullable
    private static HeldNoteSound heldOf(@Nullable final HeldNoteSound[] heldSounds, final NoteSound attack) {
        if (heldSounds == null)
            return null;
        for (final HeldNoteSound held : heldSounds)
            if (held != null && attack.equals(held.attack()))
                return held;
        return null;
    }

    private static void prewarm(final SoundManager soundManager, final RandomSource random,
                                @Nullable final SoundEvent event, final PedalChain chain, final float pitch) {
        if (event == null)
            return;

        final WeighedSoundEvents sounds = soundManager.getSoundEvent(event.getLocation());
        if (sounds != null)
            PedalBufferCache.prewarm(PedalSounds.withPedals(sounds.getSound(random), chain, pitch));
    }

    //#endregion


    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        speakers = List.of();
        monitoredSpeaker = null;
        prewarmIn = -1;
    }
}
