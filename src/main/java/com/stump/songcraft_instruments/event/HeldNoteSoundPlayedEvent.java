package com.stump.songcraft_instruments.event;

import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSounds;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * An event fired when a {@link HeldNoteSound} has been produced.
 * This event is fired on the Forge event bus
 */
@Cancelable
public class HeldNoteSoundPlayedEvent extends InstrumentPlayedEvent<HeldNoteSound> {
    public final HeldSoundPhase phase;
    /**
     * The ID of the sound initiator.
     * Can be used to access held sound instances from
     * {@link HeldNoteSounds}.
     */
    public final InitiatorID initiatorID;

    public HeldNoteSoundPlayedEvent(Level level, HeldNoteSound sound, NoteSoundMetadata soundMeta,
                                    HeldSoundPhase phase, InitiatorID initiatorID) {
        super(level, sound, soundMeta);
        this.phase = phase;
        this.initiatorID = initiatorID;
    }
    public HeldNoteSoundPlayedEvent(Entity initiator, HeldNoteSound sound, NoteSoundMetadata soundMeta,
                                    HeldSoundPhase phase, InitiatorID initiatorID) {
        this(initiator, sound, soundMeta, phase, initiatorID, PedalChain.NONE);
    }
    public HeldNoteSoundPlayedEvent(Entity initiator, HeldNoteSound sound, NoteSoundMetadata soundMeta,
                                    HeldSoundPhase phase, InitiatorID initiatorID, PedalChain pedalChain) {
        super(initiator, sound, soundMeta, pedalChain);
        this.phase = phase;
        this.initiatorID = initiatorID;
    }
}
