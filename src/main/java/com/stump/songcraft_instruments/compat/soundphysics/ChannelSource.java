package com.stump.songcraft_instruments.compat.soundphysics;

/**
 * Exposes the OpenAL source ID of a {@link com.mojang.blaze3d.audio.Channel}.
 * Implemented onto it by {@link com.stump.songcraft_instruments.mixins.soundphysics.ChannelMixin}.
 */
public interface ChannelSource {
    int songcraft_instruments$getSource();
}
