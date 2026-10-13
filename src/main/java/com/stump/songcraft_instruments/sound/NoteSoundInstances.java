package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.sound.held.InitiatorID;

import java.util.ArrayList;
import java.util.List;

public class NoteSoundInstances {

    private static final List<NoteSoundInstance> INSTANCES = new ArrayList<>();

    public static void add(NoteSoundInstance instance) {
        INSTANCES.add(instance);
    }

    public static void remove(NoteSoundInstance instance) {
        INSTANCES.remove(instance);
    }

    public static void dampenAll(final int initiatorId) {
        NearestCopyFilter.flush();
        for (NoteSoundInstance instance : new ArrayList<>(INSTANCES)) {
            if (instance.getInitiatorId().orElse(-1) == initiatorId) {
                instance.dampen();
            }
        }
    }

    public static void dampenAll(final InitiatorID initiatorId) {
        NearestCopyFilter.flush();
        for (NoteSoundInstance instance : new ArrayList<>(INSTANCES)) {
            if (instance.getOInitiatorId().map(initiatorId::equals).orElse(false)) {
                instance.dampen();
            }
        }
    }

    public static void clear() {
        NearestCopyFilter.discard();
        for (NoteSoundInstance instance : new ArrayList<>(INSTANCES)) {
            instance.stopNote();
        }

        INSTANCES.clear();
    }
}