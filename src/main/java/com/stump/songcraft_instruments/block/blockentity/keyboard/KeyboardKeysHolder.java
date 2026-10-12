package com.stump.songcraft_instruments.block.blockentity.keyboard;

/**
 * A block entity rendering a keyboard whose keys move with the notes played on it
 */
public interface KeyboardKeysHolder {
    KeyboardKeys getKeyboardKeys();

    /**
     * @return Whether this block entity currently has a keyboard to play
     */
    default boolean hasKeyboard() {
        return true;
    }
}
