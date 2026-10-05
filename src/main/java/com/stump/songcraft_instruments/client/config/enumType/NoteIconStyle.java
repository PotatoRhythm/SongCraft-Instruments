package com.stump.songcraft_instruments.client.config.enumType;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The style of the note symbols drawn on grid instrument buttons.
 */
@OnlyIn(Dist.CLIENT)
public enum NoteIconStyle {
    /** Uses whichever style the instrument specifies as its default */
    INSTRUMENT_DEFAULT,
    /** The original Genshin note symbols */
    GENSHIN,
    /** Jianpu-style numbered notation, with dots marking the lower/higher octaves */
    JIANPU;

    public String getKey() {
        return "button.songcraft_instruments.note_icon_style." + name().toLowerCase();
    }
}
