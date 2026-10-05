package com.stump.songcraft_instruments.client.gui.instrument.partial.note;

import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;
import com.stump.songcraft_instruments.networking.packet.instrument.c2s.C2SNoteSoundPacket;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.util.LabelUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;

/**
 * The abstract implementation of an instrument's note button.
 */
@OnlyIn(Dist.CLIENT)
public abstract class NoteButton extends AbstractButton {
    private static final Logger LOGGER = LogUtils.getLogger();

    protected final Minecraft minecraft = Minecraft.getInstance();

    /**
     * Returns the UI identifier of this button.
     */
    @Nullable
    public NoteButtonIdentifier getIdentifier() {
        return null;
    }


    public final InstrumentScreen instrumentScreen;
    
    private NoteSound sound;
    private NoteLabelSupplier labelSupplier;

    protected NoteButtonRenderer noteRenderer;
    protected abstract NoteButtonRenderer initNoteRenderer();


    public NoteButton(NoteSound sound, NoteLabelSupplier labelSupplier, InstrumentScreen instrumentScreen,
            int pitch) {

        super(0, 0, 42, 42, Component.empty());

        width = height = instrumentScreen.getNoteSize();

        this.sound = sound;
        this.labelSupplier = labelSupplier;
        this.instrumentScreen = instrumentScreen;
        this.pitch = pitch;
    }
    public NoteButton(NoteSound sound, NoteLabelSupplier labelSupplier, InstrumentScreen instrumentScreen) {
        this(sound, labelSupplier, instrumentScreen, instrumentScreen.getPitch());
    }

    public NoteButtonRenderer getRenderer() {
        return noteRenderer;
    }

    public void setLabelSupplier(final NoteLabelSupplier labelSupplier) {
        this.labelSupplier = labelSupplier;
        updateNoteLabel();
    }
    public NoteLabelSupplier getLabelSupplier() {
        return labelSupplier;
    }
    public void updateNoteLabel() {
        setMessage(getLabelSupplier().get(this));
    }


    public NoteSound getSound() {
        return sound;
    }
    public void setSound(NoteSound sound) {
        this.sound = sound;
    }


    /**
     * @return The position of the sounds
     * to be produced from this note button.
     */
    public BlockPos getSoundSourcePos() {
        final Player player = Minecraft.getInstance().player;

        return InstrumentOpenProvider.isItem(player)
            ? player.blockPosition()
            : InstrumentOpenProvider.getBlockPos(player)
        ;
    }


    private int initX, initY;
    private int initWidth, initHeight;
    /**
     * Initializes the button's initial position.
     * This is done for the animations to work properly - for them to stick to the same position.
     */
    public void initPos() {
        initX = getX();
        initY = getY();
        initWidth = getWidth();
        initHeight = getHeight();
    }

    public int getInitX() {
        return initX;
    }
    public int getInitY() {
        return initY;
    }
    public int getInitWidth() {
        return initWidth;
    }
    public int getInitHeight() {
        return initHeight;
    }

    public Point getCenter() {
        return ClientUtil.getInitCenter(initX, initY, instrumentScreen.getNoteSize(), width);
    }
    public void moveToCenter() {
        final Point center = getCenter();
        setPosition(center.x, center.y);
    }

    private int pitch;
    public int getPitch() {
        return pitch;
    }
    public void setPitch(final int pitch) {
        this.pitch = NoteSound.clampPitch(pitch);
        updateNoteLabel();
    }

    /**
     * @return The sound index of this note
     */
    public int soundIndex() {
        return getSound().index;
    }
    
    // Note labeling
    public NoteNotation getNotation() {
        return ModClientConfigs.ACCURATE_NOTES.get()
            ? NoteNotation.getNotation(getNoteName())
            : NoteNotation.NONE;
    }

    public String getFormattedNoteName() {
        return LabelUtil.formatNoteName(getNoteName(), true);
    }
    public String getNoteName() {
        if (instrumentScreen.noteLayout() == null)
            return "";

        return LabelUtil.getNoteName(instrumentScreen.getPitch(), instrumentScreen.noteLayout(), getNoteOffset());
    }
    /**
     * Defines the offset of this note relative to the this screen's {@link InstrumentScreen#noteLayout() note layout}
     */
    public abstract int getNoteOffset();

    /**
     * @return The vertical offset, in pixels, to draw this note's symbol at.
     * Negative values move the symbol up, away from the label.
     */
    public int getSymbolOffsetY() {
        return 0;
    }

    /**
     * @return Whether this note's button and symbol are rendered pixel-for-pixel
     * on the button's texture grid (as with unlabeled Jianpu symbols) instead of being stretched
     */
    public boolean usesPixelGridSymbol() {
        return false;
    }
    /**
     * @return The vertical offset of the symbol when {@link #usesPixelGridSymbol() drawn on the pixel grid},
     * in button texture pixels. Positive values move the symbol down.
     */
    public int getPixelGridSymbolOffsetY() {
        return 0;
    }


    public void init() {
        noteRenderer = initNoteRenderer();
        initPos();
        setLabelSupplier(labelSupplier);
    }

    public boolean isPlaying() {
        return noteRenderer.noteAnimation.isPlaying();
    }


    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        noteRenderer.render(gui, mouseX, mouseY, partialTick, instrumentScreen.getThemeLoader());
    }


    private boolean locked = false;
    public void release() {
        unlockInput();
    }

    public boolean isLocked() {
        // This comment exists for a merge to register
        return locked;
    }

    protected void lockInput() {
        locked = true;
    }
    public void unlockInput() {
        locked = false;
    }


    /**
     * Plays this note button.
     * @return Whether the operation succeed
     * @implNote Overriders should call {@link NoteButton#lockInput}
     * before a true signal.
     */
    public boolean play(final NoteSound sound, final int pitch) {
        if (locked)
            return false;

        if (instrumentScreen.getSoundOption() != null
                && instrumentScreen.getSoundOption().isSingleNote()) {
            instrumentScreen.dampenNotes();
        }

        final boolean serverAudioEnabled = ModClientConfigs.SERVER_AUDIO.get();

        if (!serverAudioEnabled) {
            playLocalSound(sound, pitch);
        }

        sendNotePlayPacket(sound, pitch);
        playNoteAnimation(false);

        lockInput();
        return true;
    }
    public boolean play() {
        return play(getSound(), getPitch());
    }

    @Override
    public void onPress() {
        play();
    }

    protected void playLocalSound(final NoteSound sound, final int pitch) {
        final BlockPos pos = getSoundSourcePos();

        sound.playLocally(
                pitch,
                instrumentScreen.volume(),
                pos,
                Minecraft.getInstance().player.position().distanceToSqr(pos.getCenter()),
                instrumentScreen.getInstrumentId(),
                Optional.ofNullable(getIdentifier())
        );
    }

    protected void sendNotePlayPacket(final NoteSound sound, final int pitch) {
        SCPacketHandler.sendToServer(new C2SNoteSoundPacket(this, sound, pitch));
    }


    public void playNoteAnimation(final boolean isForeign) {
        if (instrumentScreen.instrumentRenders())
            noteRenderer.playNoteAnimation(isForeign);
    }


    /**
     * @return The index position of this note relative to the note {@code C}
     */
    public int getABCOffset() {
        final ResourceLocation instrumentId = instrumentScreen.getInstrumentId();
        final String noteName = getNoteName();

        if (noteName.isEmpty()) {
            LOGGER.warn("Cannot get ABC offset for an instrument without a note layout! ("+instrumentId+")");
            return 0;
        }

        final char note = noteName.charAt(0);

        for (int i = 0; i < LabelUtil.ABC.length; i++)
            if (note == LabelUtil.ABC[i])
                return i;

        LOGGER.warn("Could not get note "+note+" for instrument "+instrumentScreen.getInstrumentId()+"!");
        return 0;
    }


    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        setFocused(false);
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }


    @Override
    public void playDownSound(SoundManager pHandler) {}
    

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput neo) {
        neo.add(NarratedElementType.TITLE, getMessage());
    }


    /**
     * <p>Check whether an object is equal to this {@link NoteButton}.</p>
     * 
     * An object will only be equal to this note if it is of type {@link NoteButton}
     * and both their identifiers {@link NoteButtonIdentifier#matches match}
     */
    @Override
    public boolean equals(Object obj) {
        return (this == obj) || (
            (obj instanceof NoteButton btn)
            && getIdentifier().matches(btn)
        );
    }

}