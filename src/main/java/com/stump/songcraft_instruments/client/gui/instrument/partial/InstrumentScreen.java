package com.stump.songcraft_instruments.client.gui.instrument.partial;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.SoundType;
import com.stump.songcraft_instruments.client.gui.instrument.DisclaimerScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import com.stump.songcraft_instruments.client.gui.options.partial.AbstractInstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.gui.widget.IconToggleButton;
import com.stump.songcraft_instruments.client.gui.widget.SliderButton;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.item.ModItemTags;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;
import com.stump.songcraft_instruments.networking.packet.instrument.c2s.C2SDampenNotesPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.c2s.CloseInstrumentPacket;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.sound.NoteSoundInstances;
import com.stump.songcraft_instruments.sound.SoundOption;
import com.stump.songcraft_instruments.sound.held.HeldNoteSounds;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The abstract implementation of an Instrument screen.
 */
@OnlyIn(Dist.CLIENT)
public abstract class InstrumentScreen extends Screen {
    private static final int VISIBILITY_BUTTON_MARGIN = 6;
    private static final String SPRITE_LOC = "textures/gui/sprites/";

    @SuppressWarnings("resource")
    public int getNoteSize() {
        return switch (Minecraft.getInstance().options.guiScale().get()) {
            case 1 -> 36;
            case 2 -> 46;
            case 3 -> 48;
            case 4 -> 40;
            case 5 -> 35;
            case 6 -> 30;

            default -> 35;
        };
    }

    /**
     * The set pitch of all note buttons in this screen
     */
    private int pitch;
    public int getPitch() {
        return pitch;
    }

    /**
     * Sets the pitch of all notes in this instrument
     * @param pitch The new pitch to apply
     */
    public void setPitch(int pitch) {
        this.pitch = NoteSound.clampPitch(pitch);
        notesIterable().forEach((note) -> note.setPitch(this.pitch));
    }

    /**
     * Represents the volume of this instrument in percentages (0% - 100%)
     */
    public int volume = (int)(ModClientConfigs.VOLUME.get() * 100);
    /**
     * Convenience method to get the {@link InstrumentScreen#volume volume}
     * of this instrument as a {@code float} percentage
     */
    public float volume() {
        return volume / 100f;
    }
    /**
     * Convenience method to set the {@link InstrumentScreen#volume volume}
     * of this instrument via a float percentage
     */
    public void setVolume(float volume) {
        this.volume = (int)(volume * 100);
    }


    /**
     * Sets the sounds of this instrument.
     * @apiNote This method should generally be overwritten by subclasses to keep their respected order of notes
     */
    public void setNoteSounds(final NoteSound[] sounds) {
        final Iterator<NoteButton> noteIterator = notesIterable().iterator();

        int i = 0;
        while (noteIterator.hasNext() && (i < sounds.length))
            noteIterator.next().setSound(sounds[i++]);


        if (noteIterator.hasNext() || (i < sounds.length))
            LogUtils.getLogger().warn("Not all sounds were set for instrument "+getInstrumentId()+"!");
    }

    private NoteLabelSupplier noteLabelSupplier;
    /**
     * Updates all buttons in this instrument to use
     * the specified label supplier
     * @param supplier The new supplier to use
     */
    public void setLabelSupplier(final NoteLabelSupplier supplier) {
        noteLabelSupplier = supplier;
        notesIterable().forEach((note) -> note.setLabelSupplier(supplier));
    }
    public NoteLabelSupplier getNoteLabelSupplier() {
        return noteLabelSupplier;
    }


    public abstract InstrumentThemeLoader getThemeLoader();
    public abstract ResourceLocation getInstrumentId();

    protected abstract InstrumentOptionsScreen initInstrumentOptionsScreen();


    /**
     * @return The layout of the note names across the instrument's rows.
     * Null for when this instrument does not support note names.
     * @implNote Genshin built-in instruments' layouts are derived from
     * <a href=https://github.com/Specy/genshin-music/blob/19dfe0e2fb8081508bd61dd47289dcb2d89ad5e3/src/Config.ts#L114>
     * Specy's Genshin Music app
     * </a>
     */
    public String[] noteLayout() {
        return null;
    }


    public final InstrumentMidiReceiver midiReceiver;
    /**
     * Initiates the MIDI handler of this instrument.
     * Override to implement MIDI support.
     */
    public InstrumentMidiReceiver initMidiReceiver() {
        return null;
    }

    /**
     * @return Whether this instrument can support MIDI input
     */
    public boolean isMidiInstrument() {
        return midiReceiver != null;
    }


    public void handleAbruptClosing() {
        if (!InstrumentOpenProvider.isOpen(minecraft.player))
            onClose(false);
    }


    /**
     * Uses either {@link InstrumentScreen#getNoteButton(NoteButtonIdentifier)}
     * or {@link InstrumentScreen#getNoteButton(NoteSound, int)}
     * based on whether the provided {@code noteIdentifier} is empty
     *
     * @see InstrumentScreen#identifyByPitch()
     */
    public NoteButton getNoteButton(Optional<NoteButtonIdentifier> noteIdentifier,
            NoteSound noteSound, int pitch) throws NoSuchElementException {

        if (noteIdentifier.isEmpty())
            return getNoteButton(noteSound, pitch);
        else
            return getNoteButton(noteIdentifier.get());
    }

    /**
     * @return The first {@link NoteButton} that matches the description of the given {@code noteIdentifier}.
     */
    public NoteButton getNoteButton(final NoteButtonIdentifier noteIdentifier) {
        for (NoteButton note : notesIterable())
            if (noteIdentifier.matches(note))
                return note;

        throw new NoSuchElementException("Could not find a note in "+getInstrumentId()+" based on the given identifier");
    }
    /**
     * @param noteSound The sound of the note button to find.
     * @param pitch The sound of the pitch to find.
     *
     * @return The first note button in this instrument
     * that matches the sound of the given {@code sound}.
     */
    public NoteButton getNoteButton(final NoteSound noteSound, final int pitch) {
        for (final NoteButton note : notesIterable()) {
            final NoteSound sound = note.getSound();

            if (!noteSound.equals(sound))
                continue;

            if (!identifyByPitch() || (note.getPitch() == pitch))
                return note;
        }

        throw new NoSuchElementException("Could not find a note in "+getInstrumentId()+" based on the given identifier");
    }

    /**
     * Upon {@link InstrumentScreen#getNoteButton(NoteSound, int) retrieving a note button},
     * defines whether the notes' pitch will be taken account by the comparator.
     *
     * @see InstrumentScreen#getNoteButton(NoteSound, int)
     */
    protected boolean identifyByPitch() {
        return false;
    }


    /**
     * @return A map holding an integer key as its keycode and a {@link NoteButton} as its value.
     */
    public abstract Map<Key, NoteButton> getNoteMap();
    public Iterable<NoteButton> notesIterable() {
        return getNoteMap().values();
    }

    /**
     * @return The path of the root directory of the mod
     */
    public static String getGlobalRootPath() {
        return "textures/gui/songcraft_instruments/";
    }
    /**
     * @return The resource laid inside of this instrument's directory
     */
    public ResourceLocation getResourceFromGlob(final String path) {
        return getSourcePath().withPath(getGlobalRootPath() + "instrument/" + path);
    }
    public static ResourceLocation getInternalResourceFromGlob(final String path) {
        return new ResourceLocation(SCInstrumentMod.MODID, getGlobalRootPath() + path);
    }

    public static ResourceLocation getInstrumentRootPath(final ResourceLocation instrumentId) {
        return instrumentId.withPath(getGlobalRootPath() + "instrument/" + instrumentId.getPath());
    }

    /**
     * Gets the resource path under this instrument.
     * It will usually be {@code textures/gui/songcraft_instruments/instrument/<instrument>/}.
     * {@code instrument} is as specified by {@link InstrumentScreen#getSourcePath getSourcePath}.
     */
    protected String getPath() {
        return getGlobalRootPath() + "instrument/" + getSourcePath().getPath() + "/";
    }

    /**
     * Override this method if you want to reference another directory for resources
     */
    public ResourceLocation getSourcePath() {
        return getThemeLoader().subjectInstrumentId;
    }

    public String getModId() {
        return getInstrumentId().getNamespace();
    }


    /**
     * @param path The desired path to obtain from the instrument's root directory
     * @param considerGlobal If {@link InstrumentThemeLoader#isGlobalThemed() a global resource pack is enabled}, take the resource from there
     * @return The resource contained in this instrument's root directory
     */
    public ResourceLocation getResourceFromRoot(final String path, final boolean considerGlobal) {
        return (considerGlobal && InstrumentThemeLoader.isGlobalThemed())
            ? InstrumentThemeLoader.GLOBAL_LOC.withSuffix("/" + path)
            : getSourcePath().withPath(getPath() + path);
    }
    /**
     * Gets The desired path to obtain from either the instrument's root or global directory.
     * The global directory will be used if {@link InstrumentThemeLoader#isGlobalThemed()} is true.
     * @return The resource contained in this instrument's root directory
     */
    public ResourceLocation getResourceFromRoot(final String path) {
        return getResourceFromRoot(path, true);
    }


    public final InstrumentOptionsScreen optionsScreen = initInstrumentOptionsScreen();

    public InstrumentScreen() {
        super(CommonComponents.EMPTY);
        midiReceiver = initMidiReceiver();
    }

    protected IconToggleButton visibilityButton;

    @Override
    protected void init() {
        setPitch(0);
        optionsScreen.init(minecraft, width, height);

        boolean wasEnabled = false;
        // Could be not null on screen refresh event
        if (visibilityButton != null)
            wasEnabled = visibilityButton.enabled();

        visibilityButton = initVisibilityButton();
        addRenderableWidget(visibilityButton);
        visibilityButton.setEnabled(wasEnabled);

        if (!ModClientConfigs.ACCEPTED_DISCLAIMER.get())
            minecraft.setScreen(new DisclaimerScreen(this));
    }

    protected Button initControlBar(int vertOffset) {
        Button btn = initOptionsButton(vertOffset);
        initVolumeSlider(btn);
        final SoundTypeOption<?> soundTypeOption = soundTypeOption();
        if (soundTypeOption != null) {
            AbstractButton soundTypeButton = createSoundTypeButton(soundTypeOption, 100);
            soundTypeButton.setPosition(btn.getX() + btn.getWidth() + 6, btn.getY());
            addRenderableWidget(soundTypeButton);
        }

        return btn;
    }

    protected SliderButton volumeSlider;

    protected SliderButton initVolumeSlider(Button optionsButton) {
        volumeSlider = new SliderButton(100, volume(), 0, 1) {

            @Override
            public Component getMessage() {
                return Component.translatable("button.songcraft_instruments.volume")
                        .append(": " + ((int)(value * 100)) + "%");
            }

            @Override
            protected void applyValue() {
                setVolume((float)value);

                optionsScreen.queueToSave("volume", () ->
                        ModClientConfigs.VOLUME.set(CommonUtil.round(value, 4))
                );
            }
        };

        volumeSlider.setPosition(
                optionsButton.getX() - volumeSlider.getWidth() - 6,
                optionsButton.getY()
        );

        addRenderableWidget(volumeSlider);
        return volumeSlider;
    }

    protected void changeVolume(float delta) {
        float newVolume = Math.max(0f, Math.min(1f, volume() + delta));
        setVolume(newVolume);
        volumeSlider.updateValue(newVolume);
        optionsScreen.queueToSave("volume", () ->
                ModClientConfigs.VOLUME.set(CommonUtil.round(newVolume, 4))
        );
    }

    /**
     * Initializes a new button responsible for popping up the options menu for this instrument.
     * Called during {@link Screen#init}.
     * @param vertOffset The vertical offset at which this button will be rendered.
     * @return A new Instrument Options button
     */
    protected Button initOptionsButton(final int vertOffset) {
        final Button button = Button.builder(
            Component.translatable("button.songcraft_instruments.instrumentOptions").append("..."), (btn) -> onOptionsOpen()
        )
            .width(150)
            .build();

        button.setPosition((width - button.getWidth())/2, vertOffset - button.getHeight()/2);

        addRenderableWidget(button);
        return button;
    }
    /**
     * Initialized a new button responsible for hiding the screen's GUI.
     * If enabled, the screen is hidden.
     * @return A new visibility toggle button
     */
    protected IconToggleButton initVisibilityButton() {
        return new IconToggleButton(
            VISIBILITY_BUTTON_MARGIN, VISIBILITY_BUTTON_MARGIN,
            new ResourceLocation(SCInstrumentMod.MODID, SPRITE_LOC + "enabled.png"),
            new ResourceLocation(SCInstrumentMod.MODID, SPRITE_LOC + "disabled.png"),
            (btn) -> onInstrumentRenderStateChanged(instrumentRenders())
        );
    }


    /**
     * Play a note button as a foreign
     * player. "Shared instrument screen".
     * @param event The event referring to this function
     */
    public void foreignPlay(final InstrumentPlayedEvent<?> event) {
        if (!(event instanceof NoteSoundPlayedEvent e))
            return;

        try {

            getNoteButton(
                event.soundMeta().noteIdentifier(),
                e.sound(), event.soundMeta().pitch()
            ).playNoteAnimation(true);

        } catch (Exception ignore) {
            // Button was prolly just not found
        }
    }


    /**
     * @return True whether this instrument's GUI
     * is to be rendered
     */
    public boolean instrumentRenders() {
        return !visibilityButton.enabled();
    }
    protected void onInstrumentRenderStateChanged(final boolean isVisible) {
        if (!isVisible) {
            notesIterable().forEach((note) -> note.getRenderer().resetAnimations());
        }

        renderables.forEach((renderable) -> {
            if (renderable instanceof AbstractWidget widget)
                widget.active = isVisible;
        });
        visibilityButton.active = true;
    }

    /**
     * @apiNote Prefer overwriting {@link InstrumentScreen#renderInstrument} instead.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!instrumentRenders()) {
            visibilityButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            return;
        }

        renderInstrument(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
    public void renderInstrument(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderSpeakerCounter(pGuiGraphics);
    }

    private static final ResourceLocation SPEAKER_ICON = new ResourceLocation(SCInstrumentMod.MODID, SPRITE_LOC + "speaker.png");
    private static final int SPEAKER_ICON_SIZE = 9;

    /**
     * renders the connected speaker counter ("🔈 1") in the top-right corner,
     * lined up with the visibility button. hidden when no speakers are connected.
     */
    protected void renderSpeakerCounter(final GuiGraphics gui) {
        final int count = getSpeakerCount();
        if (count <= 0)
            return;

        final String text = " " + count;
        final int iconX = width - VISIBILITY_BUTTON_MARGIN - font.width(text) - 1 - SPEAKER_ICON_SIZE;
        // vertically centered on the visibility button
        final int iconY = VISIBILITY_BUTTON_MARGIN + (18 - SPEAKER_ICON_SIZE) / 2;

        gui.blit(SPEAKER_ICON, iconX, iconY, 0, 0, SPEAKER_ICON_SIZE, SPEAKER_ICON_SIZE, SPEAKER_ICON_SIZE, SPEAKER_ICON_SIZE);
        gui.drawString(font, text, iconX + SPEAKER_ICON_SIZE + 1, iconY + 1, 0xFFFFFF);
    }
    /**
     * @return how many speakers the instrument the player has open is connected to
     */
    private int getSpeakerCount() {
        final Player player = minecraft.player;
        if (player == null)
            return 0;

        final CompoundTag modTag;
        if (InstrumentOpenProvider.isItem(player)) {
            final InteractionHand hand = InstrumentOpenProvider.getHand(player);
            // getTagElement, since apparently modTag would create the tag on the client's copy of the item every frame oops
            modTag = (hand == null) ? null : player.getItemInHand(hand).getTagElement(SCInstrumentMod.MODID);
        } else {
            final BlockPos instrumentPos = InstrumentOpenProvider.getBlockPos(player);
            final BlockEntity instrumentBE = (instrumentPos == null) ? null : player.level().getBlockEntity(instrumentPos);
            modTag = (instrumentBE == null) ? null : SCInstrumentMod.modTag(instrumentBE);
        }

        return (modTag == null) ? 0 : SpeakerUtil.getListedSpeakerCount(modTag);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (InstrumentKeyMappings.VOLUME_UP.get().matches(keyCode, scanCode)) {
            changeVolume(0.05f);
            return true;
        }
        if (InstrumentKeyMappings.VOLUME_DOWN.get().matches(keyCode, scanCode)) {
            changeVolume(-0.05f);
            return true;
        }

        if (InstrumentKeyMappings.DAMPEN.get().matches(keyCode, scanCode)) {
            dampenNotes();
            return true;
        }

        if (checkPitchTransposeUp(keyCode, scanCode))
            return true;

        final NoteButton note = getNoteByKey(keyCode);
        if (note != null) {
            note.play();
            return true;
        }

        // Arrow keys and tab are reserved for music controls (volume, octave swapping etc.),
        // so don't let them navigate between the screen's buttons
        if (isNavigationKey(keyCode))
            return true;

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isNavigationKey(final int keyCode) {
        return keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
            || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
            || keyCode == GLFW.GLFW_KEY_TAB;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        if (checkTransposeDown(pKeyCode, pScanCode))
            return true;

        // Release a focused note (in case pressed Enter)
        if (isFocused() && (getFocused() instanceof NoteButton btn))
            btn.release();

        // Filter non-instrument keys
        if (!isKeyConsumed(pKeyCode, pScanCode))
            return false;

        unlockFocused();

        final NoteButton note = getNoteByKey(pKeyCode);
        if (note != null)
            note.release();

        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    private boolean pitchChanged;
    protected boolean checkPitchTransposeUp(int pKeyCode, int pScanCode) {
        if (pitchChanged)
            return false;

        if (checkTransposeUpKey(pKeyCode, pScanCode)) {
            transposeUp();
            return true;
        }
        else if (checkTransposeDownKey(pKeyCode, pScanCode)) {
            transposeDown();
            return true;
        }

        return false;
    }
    protected boolean checkTransposeDown(int pKeyCode, int pScanCode) {
        if (!pitchChanged)
            return false;

        if (checkTransposeUpKey(pKeyCode, pScanCode) || checkTransposeDownKey(pKeyCode, pScanCode)) {
            resetTransposition();
            return true;
        }

        return false;
    }


    public void transposeUp() {
        setPitch(getPitch() + 1);
        pitchChanged = true;
    }
    public void transposeDown() {
        setPitch(getPitch() - 1);
        pitchChanged = true;
    }

    public void resetTransposition() {
        setPitch(0);
        pitchChanged = false;
    }

    /**
     * @return Whether this instrument's pitch is being transposed up/down as requested by the keybindings
     */
    public boolean isTransposed() {
        return pitchChanged;
    }


    /**
     * @return {@code true} if the given key is being used by this instrument.
     * Otherwise, {@code false}.
     */
    public boolean isKeyConsumed(final int keyCode, final int scanCode) {
        return (getNoteByKey(keyCode) != null)
            || checkTransposeDownKey(keyCode, scanCode) || checkTransposeUpKey(keyCode, scanCode);
    }

    protected boolean checkTransposeDownKey(final int keyCode, final int scanCode) {
        return InstrumentKeyMappings.TRANSPOSE_DOWN_MODIFIER.get().matches(keyCode, scanCode);
    }
    protected boolean checkTransposeUpKey(final int keyCode, final int scanCode) {
        return InstrumentKeyMappings.TRANSPOSE_UP_MODIFIER.get().matches(keyCode, scanCode);
    }


    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        unlockFocused();
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    public NoteButton getNoteByKey(final int keyCode) {
        final Key key = Type.KEYSYM.getOrCreate(keyCode);
        return getNoteMap().getOrDefault(key, null);
    }
    /**
     * Unlocks any focused {@link NoteButton}s
     */
    private void unlockFocused() {
        if ((getFocused() != null) && (getFocused() instanceof NoteButton)) {
            ((NoteButton)getFocused()).release();
            setFocused(null);
        }
    }


    private boolean isOptionsScreenActive;
    public boolean isOptionsScreenActive() {
        return isOptionsScreenActive;
    }

    public void onOptionsOpen() {
        setFocused(null);
        minecraft.pushGuiLayer(optionsScreen);
        isOptionsScreenActive = true;
    }
    public void onOptionsClose() {
        isOptionsScreenActive = false;
    }


    private boolean closed = false;

    /**
     * @apiNote Please override {@link InstrumentScreen#onClose(boolean)} instead.
     */
    @Override
    public final void onClose() {
        onClose(true);
    }
    public void onClose(final boolean notify) {
        if (!closed) {
            if (notify)
                notifyClosed();

            if (isOptionsScreenActive)
                optionsScreen.onClose();

            optionsScreen.saveOptions();
            closed = true;
        }

        super.onClose();
    }

    @Override
    public void removed() {
        // For when the screen was forcibly replaced
        if (!closed) {
            notifyClosed();
            optionsScreen.saveOptions();
            closed = true;
        }

        super.removed();
    }

    private void notifyClosed() {
        InstrumentOpenProvider.setClosed(minecraft.player);
        SCPacketHandler.sendToServer(new CloseInstrumentPacket());
    }


    /**
     * @return The current instrument screen, if present
     */
    public static Optional<InstrumentScreen> getCurrentScreen(final Minecraft minecraft) {
        if (minecraft.screen instanceof InstrumentScreen)
            return Optional.of((InstrumentScreen)minecraft.screen);

        if (minecraft.screen instanceof AbstractInstrumentOptionsScreen instrumentOptionsScreen)
            return instrumentOptionsScreen.instrumentScreen;

        return Optional.empty();
    }
    public static Optional<InstrumentScreen> getCurrentScreen() {
        return getCurrentScreen(Minecraft.getInstance());
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private SoundOption soundOption;

    /**
     * @return The sounds of this instrument's {@link #getPreferredSoundType preferred sound type} if it has one,
     * or the ones last {@link #setSoundOption set} otherwise
     */
    public SoundOption getSoundOption() {
        final SoundType soundType = getPreferredSoundType();
        return (soundType != null) ? soundType.getSoundArr().get() : soundOption;
    }

    public void setSoundOption(SoundOption option) {
        this.soundOption = option;
    }


    //#region Sound type

    /**
     * @return The sound types this instrument can switch between, or null if it has none.
     * Instruments with sound types get a button next to their volume slider to cycle through them.
     */
    public @Nullable SoundTypeOption<?> soundTypeOption() {
        return null;
    }

    private SoundType preferredSoundType;

    /**
     * @return The sound type currently chosen for this instrument, or null if it has no {@link #soundTypeOption sound types}
     */
    public @Nullable SoundType getPreferredSoundType() {
        if (preferredSoundType == null) {
            final SoundTypeOption<?> option = soundTypeOption();
            if (option != null)
                preferredSoundType = option.config().get();
        }

        return preferredSoundType;
    }
    public void setPreferredSoundType(final SoundType preferredSoundType) {
        this.preferredSoundType = preferredSoundType;
        setSoundOption(preferredSoundType.getSoundArr().get());
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T> & SoundType> CycleButton<T> createSoundTypeButton(final SoundTypeOption<T> option, final int width) {
        return CycleButton.<T>builder((soundType) ->
                Component.translatable(option.buttonKey() + "." + soundType.getName()))
                .withValues(option.values()).withInitialValue((T) getPreferredSoundType())
                .create(0, 0, width, optionsScreen.getButtonHeight(),
                        Component.translatable(option.buttonKey()),
                        (btn, soundType) -> {
                            setPreferredSoundType(soundType);
                            optionsScreen.queueToSave(getInstrumentId().getPath() + "_sound_type", () -> option.config().set(soundType));
                        }
                );
    }

    //#endregion

    public boolean isGuildWarsInstrument() {
        return ForgeRegistries.ITEMS.tags()
                .getTag(ModItemTags.GUILD_WARS_INSTRUMENTS)
                .contains(ForgeRegistries.ITEMS.getValue(getInstrumentId()));
    }

    public void dampenNotes() {
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;

        if (player == null)
            return;

        NoteSoundInstances.dampenAll(player.getId());
        HeldNoteSounds.dampenAll(InitiatorID.fromEntity(player));

        SCPacketHandler.sendToServer(
                new C2SDampenNotesPacket(player.getId())
        );
    }
}