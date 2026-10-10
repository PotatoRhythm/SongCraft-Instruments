package com.stump.songcraft_instruments.compat.soundphysics;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * The environment (muffling and reverb) and heard-from direction Sound Physics evaluated for the instrument sounds
 * coming from one place.
 * <p>Sound Physics counts occlusion in whole blocks, so stepping around a corner makes it jump between
 * "clear" and "behind a block" at once. Since it is shared by all notes from the same place, the environment
 * glides towards each new evaluation over time instead, both within a held note and across separate notes.
 * It changes at a limited rate, so no single step is big enough to be heard as a pop.</p>
 * <p>Sound thread only, except for its construction and {@link #lastUsedNanos}.</p>
 */
@OnlyIn(Dist.CLIENT)
final class EmitterEnvironment {
    /**
     * The values of Sound Physics' {@code setEnvironment}:
     * sendGain0-3, sendCutoff0-3, directCutoff, directGain
     */
    static final int VALUE_COUNT = 10;
    static final int DIRECT_CUTOFF = 8, DIRECT_GAIN = 9;
    /**
     * The index of the first cutoff value (sendCutoff0); the rest follow it up to {@link #DIRECT_CUTOFF}
     */
    static final int FIRST_CUTOFF = 4;

    /**
     * How fast the environment values may change, in decibels per second.
     * <p>OpenAL applies each new filter setting at once, so the values must change in small steps:
     * a sudden jump in muffling (especially in the high frequencies coming back) is heard as a pop,
     * and quick successive steps as clicking. At 100, going from behind a wall (~10% of the high frequencies)
     * to clear takes ~0.2 seconds, in steps of ~1dB (when applied every 10ms).</p>
     */
    private static final double MAX_DB_PER_SECOND = 100;
    /**
     * The lowest value considered when changing by decibels; anything below is treated as silent (-40dB)
     */
    private static final double DB_FLOOR = 0.01;
    /**
     * How fast the direction these sounds are heard from may turn, in degrees per second
     */
    private static final double MAX_DEGREES_PER_SECOND = 360;

    private final float[] current = new float[VALUE_COUNT];
    private final float[] target = new float[VALUE_COUNT];
    private boolean evaluated;
    private long lastUpdateNanos;

    /**
     * The direction (from the listener) Sound Physics last evaluated these sounds to be heard from,
     * or null for their real direction
     */
    private @Nullable Vec3 targetDirection;
    /**
     * The direction these sounds are currently heard from, gliding towards {@link #targetDirection}
     */
    private @Nullable Vec3 currentDirection;
    private long lastDirectionUpdateNanos;

    /**
     * A nearby environment this one starts from, so an instrument moving between blocks does not jump
     */
    private @Nullable EmitterEnvironment inheritFrom;

    volatile long lastUsedNanos = System.nanoTime();

    EmitterEnvironment(@Nullable EmitterEnvironment inheritFrom) {
        this.inheritFrom = inheritFrom;
    }

    boolean isEvaluated() {
        return evaluated;
    }

    /**
     * @return The environment Sound Physics last evaluated, unchanged by gliding. Do not modify.
     */
    float[] target() {
        lastUsedNanos = System.nanoTime();
        return target;
    }

    /**
     * Sets the environment Sound Physics newly evaluated, to glide towards
     */
    void setTarget(float[] values) {
        System.arraycopy(values, 0, target, 0, VALUE_COUNT);

        if (!evaluated) {
            final EmitterEnvironment from = inheritFrom;
            inheritFrom = null;

            final boolean inherit = (from != null) && from.evaluated;
            System.arraycopy(inherit ? from.advance() : values, 0, current, 0, VALUE_COUNT);
            if (inherit)
                currentDirection = from.currentDirection;

            lastUpdateNanos = System.nanoTime();
            evaluated = true;
        }
    }

    /**
     * Sets the direction (from the listener) Sound Physics newly evaluated these sounds to be heard from,
     * to glide towards
     * @param direction A normalized direction, or null for their real direction
     */
    void setTargetDirection(@Nullable Vec3 direction) {
        targetDirection = direction;
    }

    /**
     * Moves the direction these sounds are heard from towards their target by the time passed since it last did.
     * Shared by all notes from here, so a new note is heard from the same place as the ones already playing.
     * @param realDirection The normalized direction from the listener to where these sounds really are
     * @return The normalized direction these sounds are currently heard from
     */
    Vec3 advanceDirection(Vec3 realDirection) {
        final Vec3 target = (targetDirection != null) ? targetDirection : realDirection;
        final long now = System.nanoTime();

        if (currentDirection == null) {
            currentDirection = target;
        } else {
            final double maxAngle = Math.toRadians(MAX_DEGREES_PER_SECOND) * secondsSince(lastDirectionUpdateNanos, now);
            final double angle = Math.acos(Math.max(-1, Math.min(1, currentDirection.dot(target))));

            if (angle <= maxAngle) {
                currentDirection = target;
            } else {
                final Vec3 direction = currentDirection.lerp(target, maxAngle / angle);
                // Lerping between opposite directions may cancel out
                currentDirection = (direction.lengthSqr() < 1e-4) ? target : direction.normalize();
            }
        }

        lastDirectionUpdateNanos = now;
        return currentDirection;
    }

    /**
     * Moves the environment towards its target by the time passed since it last did
     * @return The current environment values
     */
    float[] advance() {
        final long now = System.nanoTime();
        final double maxStepDb = MAX_DB_PER_SECOND * secondsSince(lastUpdateNanos, now);

        for (int i = 0; i < VALUE_COUNT; i++)
            current[i] = approach(current[i], target[i], maxStepDb);

        lastUpdateNanos = now;
        lastUsedNanos = now;
        return current;
    }

    /**
     * @return The given value moved towards the target by at most the given amount of decibels
     */
    private static float approach(float value, float target, double maxStepDb) {
        if (value == target)
            return value;

        final double valueDb = toDb(value), diffDb = toDb(target) - valueDb;
        // Also snaps from the floor to silence
        if (Math.abs(diffDb) <= maxStepDb)
            return target;

        return (float) Math.pow(10, (valueDb + Math.signum(diffDb) * maxStepDb) / 20);
    }

    static double toDb(float value) {
        return 20 * Math.log10(Math.max(value, DB_FLOOR));
    }

    private static double secondsSince(long thenNanos, long nowNanos) {
        return (nowNanos - thenNanos) / 1e9;
    }
}
