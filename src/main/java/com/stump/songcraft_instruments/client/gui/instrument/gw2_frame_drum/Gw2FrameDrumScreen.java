package com.stump.songcraft_instruments.client.gui.instrument.gw2_frame_drum;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.LinearLayout.Orientation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class Gw2FrameDrumScreen extends InstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "gw2_frame_drum");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }


    /**
     * Maps keycodes to their respected note button
     */
    private final HashMap<Key, NoteButton> notes = new HashMap<>();
    @Override
    public Map<Key, NoteButton> getNoteMap() {
        return notes;
    }

    @Override
    protected InstrumentOptionsScreen initInstrumentOptionsScreen() {
        return new Gw2FrameDrumOptionsScreen(this);
    }

    @Override
    protected void init() {
        initControlBar(height / 2 + 5);

        final LinearLayout rowTop = createRow(1);
        final LinearLayout rowBottom = createRow(0);

        rowTop.arrangeElements();
        rowBottom.arrangeElements();

        rowTop.setPosition((width - rowTop.getWidth()) / 2, (int)(height * 0.6f));
        rowBottom.setPosition((width - rowBottom.getWidth()) / 2,
                rowTop.getY() + rowTop.getHeight() + 10);

        rowTop.arrangeElements();
        rowBottom.arrangeElements();

        rowTop.visitWidgets(this::addRenderableWidget);
        rowBottom.visitWidgets(this::addRenderableWidget);

        notesIterable().forEach(NoteButton::init);

        super.init();
    }

    private LinearLayout createRow(int row) {
        final LinearLayout layout = new LinearLayout(
                0, 0,
                (int)(width/1.5f),
                getNoteSize(),
                Orientation.HORIZONTAL
        );

        for (int column = 0; column < 5; column++) {
            createButton(layout, row, column);
        }

        return layout;
    }
    private Gw2FrameDrumNoteButton createButton(LinearLayout container, int row, int column) {
        final Gw2FrameDrumNoteButton btn = new Gw2FrameDrumNoteButton(this, row, column);

        container.addChild(btn);
        notes.put(btn.getKey(), btn);

        return btn;
    }

    public static final InstrumentThemeLoader THEME_LOADER = new InstrumentThemeLoader(INSTRUMENT_ID);
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}