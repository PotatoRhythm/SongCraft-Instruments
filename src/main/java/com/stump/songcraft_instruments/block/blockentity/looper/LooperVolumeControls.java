package com.stump.songcraft_instruments.block.blockentity.looper;

import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * The looper's volume, set with the plus and minus buttons on its side, and the animation of those buttons
 * and the indicator sliding along the scale. The animation is only used on the client.
 */
public class LooperVolumeControls {
    public static final int MAX_VOLUME = 100, VOLUME_STEP = 10;

    /**
     * How far the buttons are pushed in when pressed, and the time a press takes, in milliseconds
     */
    public static final float PRESS_DEPTH = 0.25f;
    private static final long PRESS_MILLIS = 250;
    /**
     * The time the indicator takes to slide to a new volume, in milliseconds
     */
    private static final long SLIDE_MILLIS = 300;

    /**
     * Where the indicator's center sits along the model's z axis at 0% and 100%.
     * The scale's lines in between are 1 pixel apart, the first one (10%) at z = 12.
     */
    private static final float ZERO_VOLUME_Z = 12.528125f, FULL_VOLUME_Z = 16 - ZERO_VOLUME_Z,
        FIRST_LINE_Z = 12f;

    private int volume = MAX_VOLUME;

    // In Util.getMillis() time, or NEVER
    private static final long NEVER = -1;
    private long plusPressedAt = NEVER, minusPressedAt = NEVER;
    private float slideFromZ = FULL_VOLUME_Z, slideToZ = FULL_VOLUME_Z;
    private long slideStartedAt = NEVER;
    private boolean synced = false;


    public int getVolume() {
        return volume;
    }

    /**
     * @return Whether the volume changed. It is kept between 0% and 100%, in steps of 10%
     */
    public boolean setVolume(final int volume) {
        final int newVolume = Mth.clamp(Math.round(volume / (float)VOLUME_STEP) * VOLUME_STEP, 0, MAX_VOLUME);
        if (newVolume == this.volume)
            return false;

        this.volume = newVolume;
        return true;
    }

    /**
     * Changes the volume by one step of 10%
     * @return Whether the volume changed; not when it is already at 0% or 100%
     */
    public boolean stepVolume(final boolean up) {
        return setVolume(volume + (up ? VOLUME_STEP : -VOLUME_STEP));
    }

    /**
     * Updates the volume to one sent by the server, sliding the indicator over to it.
     * The first volume received is shown right away.
     */
    public void syncVolume(final int volume) {
        final long now = Util.getMillis();
        final float fromZ = getIndicatorZ(now);
        setVolume(volume);

        if (synced) {
            slideFromZ = fromZ;
            slideStartedAt = now;
        } else {
            slideFromZ = indicatorZFor(this.volume);
            synced = true;
        }
        slideToZ = indicatorZFor(this.volume);
    }

    public void onButtonPressed(final boolean plus) {
        if (plus)
            plusPressedAt = Util.getMillis();
        else
            minusPressedAt = Util.getMillis();
    }


    /**
     * @return Where the indicator's center sits along the model's z axis for the given volume
     */
    private static float indicatorZFor(final int volume) {
        if (volume <= 0)
            return ZERO_VOLUME_Z;
        if (volume >= MAX_VOLUME)
            return FULL_VOLUME_Z;
        return FIRST_LINE_Z - (volume / VOLUME_STEP - 1);
    }

    public float getIndicatorOffset(final long now) {
        return getIndicatorZ(now) - ZERO_VOLUME_Z;
    }

    private float getIndicatorZ(final long now) {
        if (slideStartedAt == NEVER)
            return slideToZ;

        final float progress = Mth.clamp((now - slideStartedAt) / (float)SLIDE_MILLIS, 0, 1);
        // Ease out, slowing down as it settles on the line
        final float eased = 1 - (1 - progress) * (1 - progress) * (1 - progress);
        return Mth.lerp(eased, slideFromZ, slideToZ);
    }

    /**
     * @return How far the plus (or minus) button is pushed in, from 0 to {@link #PRESS_DEPTH}
     */
    public float getPressDepth(final boolean plus, final long now) {
        final long pressedAt = plus ? plusPressedAt : minusPressedAt;
        if (pressedAt == NEVER)
            return 0;

        final float progress = (now - pressedAt) / (float)PRESS_MILLIS;
        if (progress < 0 || progress >= 1)
            return 0;

        // Pushed in quickly, held for a moment, then springs back out
        final float depth;
        if (progress < 0.2f)
            depth = progress / 0.2f;
        else if (progress < 0.5f)
            depth = 1;
        else
            depth = 1 - (progress - 0.5f) / 0.5f;

        return depth * PRESS_DEPTH;
    }
}
