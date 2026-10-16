package com.stump.songcraft_instruments.event;

import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.Optional;

/**
 * An abstract implementation of a sound played event.
 * @param <T> The sound object type
 */
@Cancelable
public abstract class InstrumentPlayedEvent<T> extends Event {

    private final T sound;
    private final NoteSoundMetadata soundMeta;
    private final Level level;
    /**
     * The effects pedals the note is played through
     */
    private final PedalChain pedalChain;

    /**
     * Information about the entity initiator.
     * Present if there is indeed an entity initiator.
     */
    private final Optional<EntityInfo> entityInfo;
    public boolean isByEntity() {
        return entityInfo.isPresent();
    }
    public boolean isByPlayer() {
        return isByEntity() && (entityInfo.get().entity instanceof Player);
    }


    /**
     * Constructor for creating a non-entity event
     */
    public InstrumentPlayedEvent(Level level, T sound, NoteSoundMetadata soundMeta) {
        this(level, sound, soundMeta, PedalChain.NONE);
    }
    /**
     * Constructor for creating a non-entity event, played through the given pedals
     */
    public InstrumentPlayedEvent(Level level, T sound, NoteSoundMetadata soundMeta, PedalChain pedalChain) {
        this.level = level;
        this.sound = sound;
        this.soundMeta = soundMeta;
        this.pedalChain = pedalChain;

        this.entityInfo = Optional.empty();
    }

    /**
     * Constructor for creating a by-entity event
     */
    public InstrumentPlayedEvent(Entity entity, T sound, NoteSoundMetadata soundMeta) {
        this(entity, sound, soundMeta, PedalChain.NONE);
    }
    /**
     * Constructor for creating a by-entity event, played through the given pedals
     */
    public InstrumentPlayedEvent(Entity entity, T sound, NoteSoundMetadata soundMeta, PedalChain pedalChain) {
        this.level = entity.level();
        this.sound = sound;
        this.soundMeta = soundMeta;
        this.pedalChain = pedalChain;

        this.entityInfo = Optional.of(new EntityInfo(entity));
    }


    public T sound() {
        return sound;
    }
    public NoteSoundMetadata soundMeta() {
        return soundMeta;
    }
    public Level level() {
        return level;
    }
    /**
     * @return The effects pedals the note is played through. Only known on the server; none on the client.
     */
    public PedalChain pedalChain() {
        return pedalChain;
    }
    public Optional<EntityInfo> entityInfo() {
        return entityInfo;
    }

    /**
     * Convenience method to convert the volume of the note
     * into a {@code float} percentage
     */
    public float volume() {
        return soundMeta.volume() / 100f;
    }


    /**
     * An object containing information
     * about the entity who initiated the event
     */
    public class EntityInfo {
        public final Entity entity;

        /**
         * The hand carrying the <b>item</b> instrument.
         * Empty for when not played by an instrument
         * or is not a player.
         */
        public final Optional<InteractionHand> hand;

        protected final InstrumentPlayedEvent<T> baseEvent = InstrumentPlayedEvent.this;

        public EntityInfo(Entity entity) {
            this.entity = entity;

            if (
                (entity instanceof Player player)
                && (InstrumentOpenProvider.isItem(player))
            ) {
                hand = Optional.of(InstrumentOpenProvider.getHand(player));
            } else {
                hand = Optional.empty();
            }
        }

        /**
         * <p>Returns whether this event was fired by an item instrument.</p>
         * A {@code false} result does NOT indicate a block instrument.
         * @see EntityInfo#isBlockInstrument
         */
        public boolean isItemInstrument() {
            return hand.isPresent();
        }
        /**
         * <p>Returns whether this event was fired by a block instrument.</p>
         * A {@code false} result does NOT indicate an instrument item.
         * @see EntityInfo#isItemInstrument()
         */
        public boolean isBlockInstrument() {
            return !isItemInstrument()
                && level.getBlockEntity(baseEvent.soundMeta.pos())
                    instanceof InstrumentBlockEntity;
        }

        /**
         * @return Whether the played sound was not produced by an instrument
         */
        public boolean isNotInstrument() {
            return !isBlockInstrument() && !isItemInstrument();
        }
    }

}
