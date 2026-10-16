package com.stump.songcraft_instruments.mixins.required;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Every sound a sound event can play, so pedal prewarming can render all of a drum pad's variants
 * (see {@code PedalMonitor})
 */
@Mixin(WeighedSoundEvents.class)
public interface WeighedSoundEventsAccessor {

    @Accessor("list")
    List<Weighted<Sound>> songcraft_instruments$getList();

}
