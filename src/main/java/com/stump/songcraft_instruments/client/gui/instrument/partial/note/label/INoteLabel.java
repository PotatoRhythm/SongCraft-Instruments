package com.stump.songcraft_instruments.client.gui.instrument.partial.note.label;


import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Map;

/**
 * An interface holding {@link NoteLabelSupplier}s for note buttons to cycle thorugh.
 */
public interface INoteLabel {
    // Useful stuff
    public static final String TRANSLATABLE_PATH = "songcraft_instruments.label.";
    public static final String BUTTON_TRANS_PATH = "button.songcraft_instruments.label.";

    public static MutableComponent upperComponent(final Component component) {
        return Component.literal(component.getString().toUpperCase());
    }


    /**
     * The QWERTY symbols of the keys whose names are spelled out, as in {@code key.keyboard.left.bracket}
     */
    Map<String, String> QWERTY_SYMBOLS = Map.ofEntries(
        Map.entry("key.keyboard.comma", ","),
        Map.entry("key.keyboard.period", "."),
        Map.entry("key.keyboard.slash", "/"),
        Map.entry("key.keyboard.semicolon", ";"),
        Map.entry("key.keyboard.apostrophe", "'"),
        Map.entry("key.keyboard.left.bracket", "["),
        Map.entry("key.keyboard.right.bracket", "]"),
        Map.entry("key.keyboard.backslash", "\\"),
        Map.entry("key.keyboard.minus", "-"),
        Map.entry("key.keyboard.equal", "="),
        Map.entry("key.keyboard.grave.accent", "`")
    );

    public static MutableComponent getQwerty(final Key key) {
        final String keyName = key.getName();
        final String symbol = QWERTY_SYMBOLS.get(keyName);

        return Component.literal((symbol != null)
            ? symbol
            // Otherwise, the QWERTY key is the last letter of the key name
            : String.valueOf(keyName.charAt(keyName.length() - 1)).toUpperCase()
        );
    }

    /**
     * @return All the values of this note label type, filtering QWERTY if already using it.
     */
    public static INoteLabel[] filterQwerty(INoteLabel[] values, INoteLabel currentLabel, INoteLabel qwerty) {
        // Ignore QWERTY if already using this layout
        // Or if the user already selected it
        if (!ClientUtil.ON_QWERTY.get() || (currentLabel.equals(qwerty)))
            return values;


        final INoteLabel[] result = new INoteLabel[values.length - 1];

        // 2nd index to not go out of bounds
        int j = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(qwerty))
                i++;

            result[j] = values[i];
            j++;
        }

        return result;
    }


    public NoteLabelSupplier getLabelSupplier();
    /**
     * @return The translation key of this label
     */
    public default String getKey() {
        return BUTTON_TRANS_PATH + toString().toLowerCase(Locale.ENGLISH);
    }
}
