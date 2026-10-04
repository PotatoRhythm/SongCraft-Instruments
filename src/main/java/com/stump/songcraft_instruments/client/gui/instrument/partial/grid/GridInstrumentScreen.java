package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.ControlModeType;
import com.stump.songcraft_instruments.client.config.enumType.NoteIconStyle;
import com.stump.songcraft_instruments.client.gui.instrument.LooperOverlayInjector;
import com.stump.songcraft_instruments.client.gui.instrument.partial.*;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.*;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.*;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.held.IHoldableNoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import com.stump.songcraft_instruments.client.gui.options.GridInstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.networking.buttonidentifier.*;
import com.stump.songcraft_instruments.sound.*;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public abstract class GridInstrumentScreen extends InstrumentScreen implements IHeldInstrumentScreen {
    public static final String[] NOTE_LAYOUT = {"C", "D", "E", "F", "G", "A", "B"};

    public static final int DEF_ROWS = 7, DEF_COLUMNS = 3;
    public static final int CLEF_WIDTH = 26, CLEF_HEIGHT = 52;

    protected AbstractLayout grid;
    public NoteGrid noteGrid;
    protected Map<Key, NoteButton> noteMap;

    private GridOctaveSwapController octaveController;
    private GridHeartopiaController heartopiaController;
    private int currentOctave = 0;
    private int minOctave;
    private int maxOctave;

    /* ============================================================
     *  Note Grid Generation
     * ============================================================ */

    public int columns() { return DEF_COLUMNS; }
    public int rows() { return DEF_ROWS; }

    public NoteGrid initNoteGrid() {
        if (isHeldInstrument())
            return new NoteGrid(this);

        return isSSTI()
                ? new NoteGrid(this, NoteSound.getMinPitch())
                : new NoteGrid(this);
    }

    protected void buildGrid() {
        this.noteGrid = initNoteGrid();
        Key[][] mappings = InstrumentKeyMappings.GENSHIN_INSTRUMENT_MAPPINGS;
        this.noteMap = noteGrid.genKeyboardMap(mappings);
        this.clearWidgets();
        this.grid = noteGrid.initNoteGridLayout(.9f, width, height);
        grid.visitWidgets(this::addRenderableWidget);
        initControlBar(grid.getY() - 15);
    }

    @Override
    protected void init() {
        updateOctaveRange(ModClientConfigs.EXTEND_RANGE.get());
        buildGrid();
        super.init();
        octaveController = new GridOctaveSwapController(this);
        heartopiaController = new GridHeartopiaController(this);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModClientConfigs.CONTROL_MODE.get() == ControlModeType.HEARTOPIA) {
            heartopiaController.handleKeyPress(keyCode, scanCode);
            // Ignore default note hotkeys in octave mode
            final NoteButton note = getNoteByKey(keyCode);
            if (note != null)
                return true;
        }
        if (ModClientConfigs.CONTROL_MODE.get() == ControlModeType.OCTAVE_SWAP) {
            octaveController.handleKeyPress(keyCode, scanCode);
            final NoteButton note = getNoteByKey(keyCode);
            if (note != null)
                return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ModClientConfigs.CONTROL_MODE.get() == ControlModeType.HEARTOPIA) {
            heartopiaController.handleKeyRelease(keyCode);
            return true;
        }
        if (ModClientConfigs.CONTROL_MODE.get() == ControlModeType.OCTAVE_SWAP) {
            octaveController.handleKeyRelease(keyCode);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /* ============================================================
     *  Note Buttons
     * ============================================================ */

    /**
     * Creates a note for a singular sound type (SSTI) instrument
     */
    public NoteGridButton createNoteButton(int row, int column, int pitch) {
        return new NoteGridButton(row, column, this, pitch);
    }
    public NoteGridButton createNoteButton(int row, int column) {
        if (isHeldInstrument()) {
            return new HeldGridNoteButton(row, column, this, getHeldNoteSounds());
        }
        return new NoteGridButton(row, column, this);
    }

    /**
     * <p>
     * If the given identifier is of type {@link NoteGridButtonIdentifier},
     * uses the optimal method to obtain the described {@link NoteButton}.
     * </p>
     * Otherwise, uses {@link InstrumentScreen#getNoteButton the regular linear method}.
     * @return The {@link NoteButton} as described by the given identifier
     */
    @Override
    public NoteButton getNoteButton(final NoteButtonIdentifier noteIdentifier) throws IndexOutOfBoundsException, NoSuchElementException {
        if (!(noteIdentifier instanceof NoteGridButtonIdentifier))
            return super.getNoteButton(noteIdentifier);

        return getNoteButton((NoteGridButtonIdentifier)noteIdentifier);
    }
    /**
     * Gets a {@link NoteButton} based on the location of the note as described by the given identifier.
     */
    public NoteButton getNoteButton(final NoteGridButtonIdentifier noteIdentifier) throws IndexOutOfBoundsException {
        return getNoteButton(noteIdentifier.row, noteGrid.getFlippedColumn(noteIdentifier.column));
    }
    public NoteButton getNoteButton(final int row, final int column) throws IndexOutOfBoundsException {
        return noteGrid.getNoteButton(row, column);
    }

    /**
     * Retrieves the MIDI note that corresponds with
     * note button at position {@code note}.
     * Starts from bottom-left corner (0, 0).
     * @param note The MIDI note to fetch
     * @return The corresponding note button
     */
    public NoteButton getNoteButtonByMIDINote(final int note) {
        final int row = note % rows();
        final int column = note / rows();

        if (column < 0 || column >= columns())
            return null;

        return getNoteButton(row, column);
    }

    @Override
    public Map<Key, NoteButton> getNoteMap() {
        return noteMap;
    }

    /* ============================================================
     *  UI / Render
     * ============================================================ */

    @Override
    protected InstrumentOptionsScreen initInstrumentOptionsScreen() {
        return new GridInstrumentOptionsScreen(this);
    }

    /**
     * @return The preferred label supplier specified in this mod's configs
     */
    public static NoteLabelSupplier getInitLabelSupplier() {
        return ModClientConfigs.GRID_LABEL_TYPE.get().getLabelSupplier();
    }

    /**
     * Override this method to change the note symbols this instrument uses
     * when {@link ModClientConfigs#NOTE_ICON_STYLE} is set to {@link NoteIconStyle#INSTRUMENT_DEFAULT}.
     * Must not return {@link NoteIconStyle#INSTRUMENT_DEFAULT}.
     */
    public NoteIconStyle getDefaultNoteIconStyle() {
        return NoteIconStyle.GENSHIN;
    }
    /**
     * @return The note symbol style to render, as resolved from the configs and this instrument's default
     */
    public NoteIconStyle getNoteIconStyle() {
        final NoteIconStyle style = ModClientConfigs.NOTE_ICON_STYLE.get();
        return (style == NoteIconStyle.INSTRUMENT_DEFAULT) ? getDefaultNoteIconStyle() : style;
    }

    @Override
    public String[] noteLayout() { return NOTE_LAYOUT; }

    /**
     * Used for background rendering while determining how deep to go down
     */
    protected int getLayerAddition(final int index) {
        return index * (getNoteSize() + NoteGrid.getPaddingVert()*2);
    }

    @Override
    public void renderInstrument(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        if (ModClientConfigs.RENDER_BACKGROUND.get())
            renderInstrumentBackground(gui);

        super.renderInstrument(gui, pMouseX, pMouseY, pPartialTick);

        if (ModClientConfigs.CONTROL_MODE.get() == ControlModeType.OCTAVE_SWAP) {
            int buttonWidth = 150, buttonHeight = 20;
            int buttonX = (width - buttonWidth) / 2;
            int buttonY = grid.getY() - 15 - buttonHeight / 2;

            String text = "Octave: " + getCurrentOctave();
            int textX = buttonX + (buttonWidth / 2) - (font.width(text) / 2);
            int textY = buttonY - 12;

            gui.drawString(font, text, textX, textY, 0xFFFFFF);
        }
    }

    /**
     * Renders the background of this grid instrument.
     */
    protected void renderInstrumentBackground(final GuiGraphics gui) {
        final int clefX = grid.getX() - getNoteSize() + 8;

        // Implement your own otherwise, idk
        if (columns() == 3) {
            renderClef(gui, 0, clefX, "treble");
            renderClef(gui, 1, clefX, "alto");
            renderClef(gui, 2, clefX, "bass");
        }

        for (int i = 0; i < columns(); i++)
            renderStaff(gui, i);
    }

    protected void renderClef(GuiGraphics gui, int index, int x, String clefName) {
        RenderSystem.enableBlend();

        gui.blit(getInternalResourceFromGlob("background/clef/"+clefName+".png"),
                x, grid.getY() + NoteGrid.getPaddingVert() + getLayerAddition(index) - 5,
                0, 0,

                CLEF_WIDTH, CLEF_HEIGHT,
                CLEF_WIDTH, CLEF_HEIGHT
        );

        RenderSystem.disableBlend();
    }

    protected void renderStaff(final GuiGraphics gui, final int index) {
        RenderSystem.enableBlend();

        gui.blit(getInternalResourceFromGlob("background/staff.png"),
                grid.getX() + 2, grid.getY() + NoteGrid.getPaddingVert() + getLayerAddition(index),
                0, 0,

                grid.getWidth() - 5, getNoteSize(),
                grid.getWidth() - 5, getNoteSize()
        );

        RenderSystem.disableBlend();
    }

    /* ============================================================
     *  Sound Configuration
     * ============================================================ */

    /**
     * <p>
     * An SSTI instrument is a Singular Sound-Type Instrument, such that
     * only the <b>first</b> note in {@link GridInstrumentScreen#getInitSounds()} will get used.
     * </p><p>
     * Notes will start with the {@link NoteSound#getMinPitch()}  set minimum pitch},
     * and increment their pitch up by 1 for every new instance.
     * </p>
     * This behaviour can be changed by overriding {@link GridInstrumentScreen#initNoteGrid}.
     */
    public boolean isSSTI() { return false; }

    @Override
    public void setPitch(int pitch) {
        if (!isSSTI())
            super.setPitch(pitch);
    }

    @Override
    protected boolean identifyByPitch() { return isSSTI(); }

    @Override
    public void setSoundOption(SoundOption option) {
        closeHeldScreen();
        super.setSoundOption(option);
        buildGrid();

        // Rebuilding the grid clears all widgets; add back the ones that aren't part of it
        if (visibilityButton != null)
            addRenderableWidget(visibilityButton);
        LooperOverlayInjector.getButtons(this).forEach(this::addRenderableWidget);
    }

    @Override
    public void setNoteSounds(NoteSound[] sounds) {
        noteGrid.setNoteSounds(sounds);
    }

    public NoteSound[] getInitSounds() {
        SoundOption opt = getSoundOption();

        if (opt.isHeld()) {
            return HeldNoteSound.getSounds(opt.getHeldSounds(), HeldNoteSound.Phase.ATTACK);
        }
        return opt.getNoteSounds();
    }

    @Override
    public void setHeldNoteSounds(HeldNoteSound[] heldSounds) {
        notesIterable().forEach(btn -> {
            if (btn instanceof IHoldableNoteButton holdableBtn) {
                int index = ((NoteGridButton) btn).posToIndex();
                holdableBtn.setHeldNoteSound(heldSounds[index]);
            }
        });
    }

    public HeldNoteSound[] getHeldNoteSounds() {
        return getSoundOption().getHeldSounds();
    }

    public boolean isHeldInstrument() {
        return getSoundOption().isHeld();
    }


    /* ============================================================
     *  Playback Stuff
     * ============================================================ */

    @Override
    public InstrumentMidiReceiver initMidiReceiver() {
        return ((rows() != 7) || isSSTI()) ? null : new GridInstrumentMidiReceiver(this);
    }

    @Override
    public void foreignPlay(final InstrumentPlayedEvent<?> event) {
        if (isHeldInstrument())
            foreignPlayHeld(event);
        else
            super.foreignPlay(event);
    }

    @Override
    public void onClose(final boolean notify) {
        if (isHeldInstrument())
            closeHeldScreen();

        super.onClose(notify);
    }

    /* ============================================================
     *  Octave Tracking
     * ============================================================ */

    public int getCurrentOctave() { return currentOctave; }

    public int getMinOctave() { return minOctave; }

    public int getMaxOctave() { return maxOctave; }

    public void setCurrentOctave(int current) { currentOctave = current; }

    public void updateOctaveRange(boolean extendRange) {
        minOctave = extendRange ? -2 : -1;
        maxOctave = extendRange ? columns() - 1 : columns() - 2;
        currentOctave = Math.max(minOctave, Math.min(currentOctave, maxOctave));
    }
}
