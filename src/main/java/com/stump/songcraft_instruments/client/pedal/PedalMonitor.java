package com.stump.songcraft_instruments.client.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.IHeldInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.mixins.required.WeighedSoundEventsAccessor;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pedals the local player hears their own notes through: those of the instrument they have open.
 * <p>A player hears their own copy of their notes without waiting on the server, so the server tells them their open
 * instrument's chain (see {@code PedalMonitorSync}), and their copy is played through it. Everyone else hears the
 * notes through the same chain, sent along with each note.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class PedalMonitor {
    /**
     * How long (in ticks) a chain must stay the same before the open instrument's sounds are rendered through it,
     * so turning a pedal's knobs doesn't render every sound at every step
     */
    private static final int PREWARM_DELAY = 10;
    /**
     * How deeply nested sound events are followed for their variants, in case of a loop
     */
    private static final int MAX_VARIANT_DEPTH = 4;

    private static PedalChain chain = PedalChain.NONE;
    private static int prewarmIn = -1;

    private PedalMonitor() {}

    public static void setChain(final PedalChain newChain) {
        chain = newChain;
        prewarmIn = chain.isEmpty() ? -1 : PREWARM_DELAY;
    }

    /**
     * @return The pedals the local player's own notes should be played through right now
     */
    public static PedalChain chain() {
        return chain;
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
        if (chain.isEmpty())
            return;

        // Each sound, at each pitch it's played at by the screen's notes (chains are rendered for it, see PedalSounds)
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
        final List<Sound> rendered = new ArrayList<>();
        for (final Played sound : played) {
            if (sound.chain().isEmpty())
                continue;
            addRendered(rendered, soundManager, random, sound.sound().getStereo(), sound.chain(), sound.pitch());
            addRendered(rendered, soundManager, random, sound.sound().getMono(), sound.chain(), sound.pitch());
        }
        PedalBufferCache.prewarm(rendered);
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

    private static void addRendered(final List<Sound> rendered, final SoundManager soundManager,
                                    final RandomSource random, @Nullable final SoundEvent event,
                                    final PedalChain chain, final float pitch) {
        if (event == null)
            return;

        final WeighedSoundEvents sounds = soundManager.getSoundEvent(event.getLocation());
        if (sounds != null)
            addVariants(rendered, sounds, chain, pitch, random, 0);
    }

    /**
     * Adds every sound the entry can play, so each of a drum pad's variants is ready, not just one of them.
     * Nested sound events are followed; any other kind of entry (like vanilla's references to other events)
     * only has one of its sounds picked.
     */
    private static void addVariants(final List<Sound> rendered, final Weighted<Sound> entry, final PedalChain chain,
                                    final float pitch, final RandomSource random, final int depth) {
        if (entry instanceof Sound sound) {
            rendered.add(PedalSounds.withPedals(sound, chain, pitch));
        } else if (entry instanceof WeighedSoundEvents events && depth < MAX_VARIANT_DEPTH) {
            for (final Weighted<Sound> variant : ((WeighedSoundEventsAccessor) events).songcraft_instruments$getList())
                addVariants(rendered, variant, chain, pitch, random, depth + 1);
        } else {
            rendered.add(PedalSounds.withPedals(entry.getSound(random), chain, pitch));
        }
    }

    //#endregion


    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        chain = PedalChain.NONE;
        prewarmIn = -1;
    }
}
