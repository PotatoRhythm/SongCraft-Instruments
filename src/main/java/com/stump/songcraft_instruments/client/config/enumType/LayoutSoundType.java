package com.stump.songcraft_instruments.client.config.enumType;

/**
 * A sound type that knows how it's laid out on its instrument's screen:
 * how many rows of notes it fills in the note grid, or that it's played on pads instead.
 * @see com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen
 */
public interface LayoutSoundType extends SoundType {
    /**
     * Row count of the sound types played on pads, laid out in 2 rows from the top left
     */
    int PADS = 0;
    /**
     * Row count of the sound types played on pads laid out like the Djem Djem Drum's, as the Heartopia drums are
     */
    int DRUM_PADS = -1;

    /**
     * @return The number of note rows this sound type fills in the note grid, or {@link #PADS}/{@link #DRUM_PADS}
     */
    int rows();

    /**
     * @return Whether this sound type is played on pads rather than the note grid
     */
    default boolean isPads() {
        return rows() <= PADS;
    }
}
