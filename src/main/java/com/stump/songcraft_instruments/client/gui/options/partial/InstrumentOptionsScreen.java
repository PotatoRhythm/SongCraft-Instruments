package com.stump.songcraft_instruments.client.gui.options.partial;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.options.MidiOptionsScreen;
import com.stump.songcraft_instruments.client.gui.options.ParticleEditorScreen;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.LinearLayout.Orientation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * The base class for all main instrument options screens.
 * Includes basic configurations all instruments should share
 * by default.
 */
@OnlyIn(Dist.CLIENT)
public abstract class InstrumentOptionsScreen extends AbstractInstrumentOptionsScreen {
    public static final MutableComponent MIDI_OPTIONS = Component.translatable("label.songcraft_instruments.midiOptions");

    private static final String STOP_MUSIC_KEY = "button.songcraft_instruments.stop_music_on_play";


    public abstract INoteLabel[] getLabels();
    /**
     * @return The current note label for this instrument's notes
     */
    public abstract INoteLabel getCurrentLabel();
    

    protected final @Nullable INoteLabel[] labels;
    protected @Nullable INoteLabel currLabel;

    public InstrumentOptionsScreen(@Nullable InstrumentScreen screen) {
        super(Component.translatable("button.songcraft_instruments.instrumentOptions"), screen);
        labels = getLabels();
    }
    public InstrumentOptionsScreen(final Screen lastScreen) {
        super(Component.translatable("button.songcraft_instruments.instrumentOptions"), lastScreen);
        labels = getLabels();
    }

    @Override
    protected void init() {
        currLabel = getCurrentLabel();

        final GridLayout grid = ClientUtil.createSettingsGrid();

        initOptionsGrid(grid, grid.createRowHelper(2));

        final int buttonsY = arrangeVertically(grid);
        grid.visitWidgets(this::addRenderableWidget);

        final int buttonsWidth = 150;

        final Button doneBtn = Button.builder(CommonComponents.GUI_DONE, (btn) -> onClose())
            .width(buttonsWidth)
            .build();

        // Add MIDI options button for MIDI instruments
        if (!isOverlay || instrumentScreen.get().isMidiInstrument()) {
            final LinearLayout buttonLayout = new LinearLayout(
                grid.getX() + getSmallButtonWidth() - buttonsWidth + ClientUtil.GRID_HORZ_PADDING, buttonsY,
                (buttonsWidth + ClientUtil.GRID_HORZ_PADDING) * 2, getButtonHeight(),
                Orientation.HORIZONTAL
            );

            final Button midiOptions = Button.builder(MIDI_OPTIONS.copy().append("..."), (btn) -> openMidiOptions())
                .width(buttonsWidth)
                .build();

            buttonLayout.addChild(midiOptions);
            buttonLayout.addChild(doneBtn);

            buttonLayout.arrangeElements();
            buttonLayout.visitWidgets(this::addRenderableWidget);
        } else {
            doneBtn.setPosition((width - doneBtn.getWidth())/2, buttonsY);
            addRenderableWidget(doneBtn);
        }

    }

    protected void initVisualsSection(final GridLayout grid, final RowHelper rowHelper) {

        // Not visual, but no space
        final CycleButton<Boolean> stopMusic = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
            .withInitialValue(ModClientConfigs.STOP_MUSIC_ON_PLAY.get())
            .withTooltip((value) -> Tooltip.create(Component.translatable(STOP_MUSIC_KEY+".tooltip", ClientUtil.STOP_SOUND_DISTANCE)))
            .create(0, 0,
                getSmallButtonWidth(), getButtonHeight(),
                Component.translatable(STOP_MUSIC_KEY), this::onMusicStopChanged
            );
        rowHelper.addChild(stopMusic);

        final CycleButton<Boolean> sharedInstrument = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
            .withInitialValue(ModClientConfigs.SHARED_INSTRUMENT.get())
            .withTooltip((value) -> Tooltip.create(Component.translatable("button.songcraft_instruments.shared_instrument.tooltip")))
            .create(0, 0,
                getSmallButtonWidth(), getButtonHeight(),
                Component.translatable("button.songcraft_instruments.shared_instrument"), this::onSharedInstrumentChanged
            );
        rowHelper.addChild(sharedInstrument);

        final CycleButton<Boolean> accurateNotes = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
            .withInitialValue(ModClientConfigs.ACCURATE_NOTES.get())
            .withTooltip((value) -> Tooltip.create(Component.translatable("button.songcraft_instruments.accurate_notes.tooltip")))
            .create(0, 0,
                getSmallButtonWidth(), getButtonHeight(),
                Component.translatable("button.songcraft_instruments.accurate_notes"), this::onAccurateNotesChanged
            );
        rowHelper.addChild(accurateNotes);

        if (labels != null) {
            final CycleButton<INoteLabel> labelType = CycleButton.<INoteLabel>builder((label) -> Component.translatable(label.getKey()))
                .withValues(labels)
                .withInitialValue(currLabel)
                .withTooltip((value) -> Tooltip.create(Component.translatable(value.getKey()+".description")))
                .create(0, 0,
                    getSmallButtonWidth(), getButtonHeight(),
                    Component.translatable("button.songcraft_instruments.label"), this::onLabelChanged
                );
        rowHelper.addChild(labelType);
        }

        final CycleButton<Boolean> serverAudio = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
                .withInitialValue(ModClientConfigs.SERVER_AUDIO.get())
                .withTooltip((value) -> Tooltip.create(Component.translatable("button.songcraft_instruments.server_audio.tooltip")))
                .create(0, 0,
                        getSmallButtonWidth(), getButtonHeight(),
                        Component.translatable("button.songcraft_instruments.server_audio"), this::onServerAudioChanged
                );
        rowHelper.addChild(serverAudio);

        final Button particleColorEditorBtn = Button.builder(
                        Component.translatable("button.songcraft_instruments.particle_color_editor"),
                        (btn) -> openParticleColorEditor()
                )
                .width(getSmallButtonWidth())
                .build();

        rowHelper.addChild(particleColorEditorBtn);

    }

    // Hook method for subclasses to add instrument-specific controls
    protected void initControlSection(final GridLayout grid, final RowHelper rowHelper) { }

    /**
     * Hook for subclasses to add a centered row below all other options.
     * Subclasses that add to it should start it with a spacer, to separate it from the rest of the options.
     */
    protected void initBottomSection(final GridLayout grid, final RowHelper rowHelper) { }

    /**
     * Fills the settings grid with all the necessary widgets, buttons and such
     * @param grid The settings grid to add the widgets to
     * @param rowHelper A row helper for the specified {@code grid}
     */
    protected void initOptionsGrid(final GridLayout grid, final RowHelper rowHelper) {
        initVisualsSection(grid, rowHelper);
        initControlSection(grid, rowHelper);
        initBottomSection(grid, rowHelper);
    }

    // The label enum is not cached anywhere; just save it.
    protected void onLabelChanged(final CycleButton<INoteLabel> button, final INoteLabel label) {
        instrumentScreen.ifPresent((screen) -> screen.setLabelSupplier(label.getLabelSupplier()));
        saveLabel(label);
    }
    protected abstract void saveLabel(final INoteLabel newLabel);

    // These values derive from the config directly, so just update them on-spot
    protected void onMusicStopChanged(final CycleButton<Boolean> button, final boolean value) {
        ModClientConfigs.STOP_MUSIC_ON_PLAY.set(value);
    }
    protected void onSharedInstrumentChanged(final CycleButton<Boolean> button, final boolean value) {
        ModClientConfigs.SHARED_INSTRUMENT.set(value);
    }
    protected void onAccurateNotesChanged(final CycleButton<Boolean> button, final boolean value) {
        ModClientConfigs.ACCURATE_NOTES.set(value);

        instrumentScreen.ifPresent((screen) ->
            screen.notesIterable().forEach(NoteButton::updateNoteLabel)
        );
    }
    protected void onServerAudioChanged(final CycleButton<Boolean> button, final boolean value) {
        ModClientConfigs.SERVER_AUDIO.set(value);
    }

    protected void openParticleColorEditor() {
        Screen particleEditorScreen = new ParticleEditorScreen(this);
        if (isOverlay) {
            minecraft.popGuiLayer();
            minecraft.pushGuiLayer(particleEditorScreen);
        } else {
            minecraft.setScreen(particleEditorScreen);
        }
    }

    protected void openMidiOptions() {
        if (isOverlay) {
            minecraft.popGuiLayer();
            minecraft.pushGuiLayer(midiOptionsScreen());
        } else
            minecraft.setScreen(midiOptionsScreen());
    }

    protected MidiOptionsScreen midiOptionsScreen() {
        return new MidiOptionsScreen(MIDI_OPTIONS, this, instrumentScreen);
    }


    @Override
    public void onClose() {
        super.onClose();
        instrumentScreen.ifPresent(InstrumentScreen::onOptionsClose);
    }
}