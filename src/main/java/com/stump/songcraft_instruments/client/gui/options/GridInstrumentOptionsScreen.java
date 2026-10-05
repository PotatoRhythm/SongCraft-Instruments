package com.stump.songcraft_instruments.client.gui.options;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.ControlModeType;
import com.stump.songcraft_instruments.client.config.enumType.NoteGridLabel;
import com.stump.songcraft_instruments.client.config.enumType.NoteIconStyle;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.gui.widget.SliderButton;
import com.stump.songcraft_instruments.util.LabelUtil;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.MOD, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class GridInstrumentOptionsScreen extends InstrumentOptionsScreen {

    public GridInstrumentOptionsScreen(final GridInstrumentScreen screen) {
        super(screen);
    }
    public GridInstrumentOptionsScreen(final Screen lastScreen) {
        super(lastScreen);
    }

    @Override
    public INoteLabel[] getLabels() {
        return NoteGridLabel.availableVals();
    }
    @Override
    public INoteLabel getCurrentLabel() {
        return ModClientConfigs.GRID_LABEL_TYPE.get();
    }

    @Override
    protected void saveLabel(final INoteLabel newLabel) {
        if (newLabel instanceof NoteGridLabel)
            ModClientConfigs.GRID_LABEL_TYPE.set((NoteGridLabel)newLabel);
    }

    @Override
    protected void initVisualsSection(GridLayout grid, RowHelper rowHelper) {
        final CycleButton<Boolean> renderBackground = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF)
            .withInitialValue(ModClientConfigs.RENDER_BACKGROUND.get())
            .create(0, 0,
                getSmallButtonWidth(), getButtonHeight(),
                Component.translatable("button.songcraft_instruments.render_background"), this::onRenderBackgroundChanged
            );
        rowHelper.addChild(renderBackground);

        final CycleButton<NoteIconStyle> noteIconStyle = CycleButton.<NoteIconStyle>builder((style) -> Component.translatable(style.getKey()))
            .withValues(NoteIconStyle.values())
            .withInitialValue(ModClientConfigs.NOTE_ICON_STYLE.get())
            .withTooltip((value) -> Tooltip.create(Component.translatable(value.getKey()+".description")))
            .create(0, 0,
                getSmallButtonWidth(), getButtonHeight(),
                Component.translatable("button.songcraft_instruments.note_icon_style"), this::onNoteIconStyleChanged
            );
        rowHelper.addChild(noteIconStyle);

        super.initVisualsSection(grid, rowHelper);
    }

    // Note symbols are resolved from the config on every render, so just save it
    protected void onNoteIconStyleChanged(final CycleButton<NoteIconStyle> button, final NoteIconStyle value) {
        ModClientConfigs.NOTE_ICON_STYLE.set(value);
    }

    @Override
    protected void initControlSection(GridLayout grid, RowHelper rowHelper) {
        CycleButton<ControlModeType> controlModeButton = CycleButton.<ControlModeType>builder(mode ->
                        Component.translatable("button.songcraft_instruments.control_mode." + mode.name().toLowerCase())
                )
                .withValues(ControlModeType.values())
                .withInitialValue(ModClientConfigs.CONTROL_MODE.get()) // optional persistent config
                .withTooltip((value) -> Tooltip.create(Component.translatable(value.getKey()+".description")))
                .create(0, 0,
                        getSmallButtonWidth(), getButtonHeight(), Component.translatable("button.songcraft_instruments.control_mode"), this::onControlModeChanged
                );
        rowHelper.addChild(controlModeButton);

        super.initControlSection(grid, rowHelper);
    }

    @Override
    protected void initBottomSection(GridLayout grid, RowHelper rowHelper) {
        rowHelper.addChild(SpacerElement.height(7), 2);


        // 1.4x the width of a regular button, centered across both columns
        final SliderButton transpose = new SliderButton(getSmallButtonWidth() * 7 / 5,
                ModClientConfigs.TRANSPOSE.get(), -12, 12) {
            @Override
            public Component getMessage() {
                int value = (int) Math.round(getValueClamped());
                return Component.translatable("button.songcraft_instruments.transpose").append(": ").append(getTransposeDisplay(value));
            }

            @Override
            protected void applyValue() {
                int value = (int) Math.round(getValueClamped());
                ModClientConfigs.TRANSPOSE.set(value);

                // The ABC labels show the transposed notes
                instrumentScreen.ifPresent((screen) ->
                    screen.notesIterable().forEach(NoteButton::updateNoteLabel)
                );
            }
        };
        rowHelper.addChild(transpose, 2, rowHelper.newCellSettings().alignHorizontallyCenter());
    }

    protected void onRenderBackgroundChanged(final CycleButton<Boolean> button, final boolean value) {
        ModClientConfigs.RENDER_BACKGROUND.set(value);
    }

    protected void onControlModeChanged(final CycleButton<ControlModeType> button, final ControlModeType value) {
        ModClientConfigs.CONTROL_MODE.set(value);

        // Each control mode plays the notes with different keys
        instrumentScreen.ifPresent((screen) ->
            screen.notesIterable().forEach(NoteButton::updateNoteLabel)
        );
    }

    // Register this options type as the main configs
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenFactory.class,
            () -> new ConfigScreenFactory((minecraft, screen) -> new GridInstrumentOptionsScreen(screen))
        );
    }

    private static String getTransposeDisplay(int transpose) {
        String sign = transpose > 0 ? "+" : "";
        String note = LabelUtil.getKeyName(transpose);

        return sign + transpose + "  [" + note + "]";
    }
}
