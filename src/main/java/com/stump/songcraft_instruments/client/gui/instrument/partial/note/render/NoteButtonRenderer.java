package com.stump.songcraft_instruments.client.gui.instrument.partial.note.render;

import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.animation.NoteAnimationController;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class NoteButtonRenderer {
    protected static final Minecraft MINECRAFT = Minecraft.getInstance();

    private static final double SHARP_MULTIPLIER = .9;
    protected static final double NOTE_DUR = .15, NOTE_TARGET_VAL = 9;
    
    public final NoteButton noteButton;
    protected final InstrumentScreen instrumentScreen;

    // Resources
    protected ResourceLocation rootLocation, accidentalsLocation;
    protected ResourceLocation notePressedLocation, noteReleasedLocation, noteHoverLocation;
    protected static final ResourceLocation GW2_NOTE_ROOT = new ResourceLocation(
            "songcraft_instruments",
            "textures/gui/songcraft_instruments/instrument/gw2"
    );

    /** How far below the button's center the top of the label is drawn */
    public static final int LABEL_OFFSET_Y = 7;

    // Jianpu style; drawn 1:1 on the button's texture pixel grid
    /** The size of the default button texture, which defines the pixel grid */
    public static final int BUTTON_TEXTURE_SIZE = 16;
    public static final int JIANPU_SYMBOL_WIDTH = 7, JIANPU_SYMBOL_HEIGHT = 9;
    /** Odd-sized so that the 1-pixel-wide symbols have a true center */
    protected static final int JIANPU_BUTTON_SIZE = 15;
    /** A single pixel, drawn above or below a Jianpu symbol to mark its octave */
    protected static final ResourceLocation OCTAVE_DOT_LOCATION =
        InstrumentScreen.getInternalResourceFromGlob("note/label/grid_generic/dot.png");

    protected ResourceLocation jianpuPressedLocation, jianpuReleasedLocation, jianpuHoverLocation;
    protected int jianpuButtonSize;

    protected Supplier<ResourceLocation> labelTextureProvider;

    // Animations
    public final NoteAnimationController noteAnimation;
    public boolean foreignPlaying = false;
    protected final ArrayList<NoteRing> rings = new ArrayList<>();

    protected NoteAnimationController initNoteAnimation() {
        return new NoteAnimationController(NOTE_DUR, NOTE_TARGET_VAL, noteButton);
    }

    public NoteButtonRenderer(NoteButton noteButton, Supplier<ResourceLocation> labelTextureProvider) {
        this.noteButton = noteButton;
        this.labelTextureProvider = labelTextureProvider;
        this.instrumentScreen = noteButton.instrumentScreen;

        noteAnimation = initNoteAnimation();
        rootLocation = instrumentScreen.getResourceFromRoot("note");

        String[] notes = {"c", "d", "e", "f", "g", "a", "b"};
        if (instrumentScreen.isGuildWarsInstrument()) {
            int index = noteButton.soundIndex();
            String noteLetter = notes[index % 7];

            accidentalsLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "accidentals_" + noteLetter + ".png");
            noteReleasedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "released_" + noteLetter + ".png");
            notePressedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "pressed_" + noteLetter + ".png");
            noteHoverLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "hovered_" + noteLetter + ".png");
        } else {
            accidentalsLocation = getResourceFromRoot("accidentals.png");
            noteReleasedLocation = getResourceFromRoot("note/released.png");
            notePressedLocation = getResourceFromRoot("note/pressed.png");
            noteHoverLocation = getResourceFromRoot("note/hovered.png");
        }

        initJianpuLocations();
    }

    /**
     * Uses the instrument's {@code note/note_jianpu} buttons if it has them,
     * otherwise falls back to its regular buttons.
     */
    protected void initJianpuLocations() {
        final ResourceLocation released = getResourceFromRoot("note_jianpu/released.png");

        if (MINECRAFT.getResourceManager().getResource(released).isPresent()) {
            jianpuReleasedLocation = released;
            jianpuPressedLocation = getResourceFromRoot("note_jianpu/pressed.png");
            jianpuHoverLocation = getResourceFromRoot("note_jianpu/hovered.png");
            jianpuButtonSize = JIANPU_BUTTON_SIZE;
        } else {
            jianpuReleasedLocation = noteReleasedLocation;
            jianpuPressedLocation = notePressedLocation;
            jianpuHoverLocation = noteHoverLocation;
            jianpuButtonSize = BUTTON_TEXTURE_SIZE;
        }
    }

    protected ResourceLocation getNotePressedLocation() {
        return notePressedLocation;
    }
    protected ResourceLocation getNoteReleasedLocation() {
        return noteReleasedLocation;
    }
    protected ResourceLocation getNoteHoverLocation() {
        return noteHoverLocation;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick, InstrumentThemeLoader themeLoader) {
        RenderSystem.enableBlend();

        rings.removeIf((ring) -> !ring.isPlaying());
        rings.forEach((ring) -> ring.render(gui, themeLoader));


        renderNoteButton(gui, themeLoader);
        renderNoteSymbol(gui, themeLoader);
        renderLabel(gui, themeLoader);
        
        renderAccidentals(gui, themeLoader);


        noteAnimation.update();
    }

    protected void renderNoteButton(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        if (noteButton.usesPixelGridSymbol()) {
            renderJianpuNoteButton(gui);
            return;
        }

        ResourceLocation noteLocation;

        if (noteButton.isPlaying()) {

            if (foreignPlaying)
                noteLocation = getNoteHoverLocation();
            else
                noteLocation = getNotePressedLocation();

        } else if (noteButton.isHoveredOrFocused())
            noteLocation = getNoteHoverLocation();
        else
            noteLocation = getNoteReleasedLocation();
            
        
        gui.blit(noteLocation,
            noteButton.getX(), noteButton.getY(),
            0, 0,

            noteButton.getWidth(), noteButton.getHeight(),
            noteButton.getWidth(), noteButton.getHeight()
        );
    }

    protected void renderNoteSymbol(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        if (instrumentScreen.isGuildWarsInstrument())
            return;

        final int noteWidth = noteButton.getWidth()/2, noteHeight = noteButton.getHeight()/2;

        ClientUtil.setShaderColor((noteButton.isPlaying() && !foreignPlaying)
            ? themeLoader.notePressed(noteButton)
            : themeLoader.noteReleased(noteButton)
        );

        if (noteButton.usesPixelGridSymbol()) {
            renderJianpuSymbol(gui);
            ClientUtil.resetShaderColor();
            return;
        }

        final int symbolX = noteButton.getX() + noteWidth/2,
            symbolY = noteButton.getY() + noteHeight/2 + noteButton.getSymbolOffsetY();

        gui.blit(labelTextureProvider.get(),
            symbolX, symbolY,
            0, 0,

            noteWidth, noteHeight,
            noteWidth, noteButton.getHeight()/2
        );

        // The symbol is stretched over its box, so stretch the dot along with it
        if (noteButton.getOctaveDot() != 0) {
            gui.pose().pushPose();
            gui.pose().translate(symbolX, symbolY, 0);
            gui.pose().scale(noteWidth / (float) JIANPU_SYMBOL_WIDTH, noteHeight / (float) JIANPU_SYMBOL_HEIGHT, 1);
            gui.blit(OCTAVE_DOT_LOCATION, JIANPU_SYMBOL_WIDTH / 2, getOctaveDotRow(), 0, 0, 1, 1, 1, 1);
            gui.pose().popPose();
        }

        ClientUtil.resetShaderColor();
    }

    /**
     * @return The row of the symbol's texture that its octave dot sits on
     */
    protected int getOctaveDotRow() {
        return (noteButton.getOctaveDot() > 0) ? 0 : (JIANPU_SYMBOL_HEIGHT - 1);
    }

    protected void renderJianpuNoteButton(final GuiGraphics gui) {
        final ResourceLocation noteLocation;

        if (noteButton.isPlaying())
            noteLocation = foreignPlaying ? jianpuHoverLocation : jianpuPressedLocation;
        else if (noteButton.isHoveredOrFocused())
            noteLocation = jianpuHoverLocation;
        else
            noteLocation = jianpuReleasedLocation;

        final float offset = (BUTTON_TEXTURE_SIZE - jianpuButtonSize) / 2f;
        blitOnPixelGrid(gui, noteLocation, offset, offset, jianpuButtonSize, jianpuButtonSize);
    }

    protected void renderJianpuSymbol(final GuiGraphics gui) {
        final float buttonOffset = (BUTTON_TEXTURE_SIZE - jianpuButtonSize) / 2f;

        // Center on whole pixels, so the symbol's pixels line up with the button's
        final float x = buttonOffset + (jianpuButtonSize - JIANPU_SYMBOL_WIDTH) / 2;
        final float y = buttonOffset + (jianpuButtonSize - JIANPU_SYMBOL_HEIGHT) / 2
            + noteButton.getPixelGridSymbolOffsetY();

        blitOnPixelGrid(gui, labelTextureProvider.get(), x, y, JIANPU_SYMBOL_WIDTH, JIANPU_SYMBOL_HEIGHT);

        if (noteButton.getOctaveDot() != 0)
            blitOnPixelGrid(gui, OCTAVE_DOT_LOCATION, x + JIANPU_SYMBOL_WIDTH / 2, y + getOctaveDotRow(), 1, 1);
    }

    /**
     * Draws a texture at the scale of the button's {@link #BUTTON_TEXTURE_SIZE 16x16} pixel grid,
     * so each texture pixel covers exactly one button pixel.
     * @param pixelX The horizontal position, in button pixels
     * @param pixelY The vertical position, in button pixels
     */
    protected void blitOnPixelGrid(final GuiGraphics gui, final ResourceLocation texture,
            final float pixelX, final float pixelY, final int textureWidth, final int textureHeight) {
        final float pixelWidth = noteButton.getWidth() / (float) BUTTON_TEXTURE_SIZE,
            pixelHeight = noteButton.getHeight() / (float) BUTTON_TEXTURE_SIZE;

        gui.pose().pushPose();
        gui.pose().translate(noteButton.getX() + pixelX * pixelWidth, noteButton.getY() + pixelY * pixelHeight, 0);
        gui.pose().scale(pixelWidth, pixelHeight, 1);

        gui.blit(texture,
            0, 0,
            0, 0,

            textureWidth, textureHeight,
            textureWidth, textureHeight
        );

        gui.pose().popPose();
    }

    protected void renderLabel(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        gui.drawCenteredString(
            MINECRAFT.font, noteButton.getMessage(),
            noteButton.getInitX() + noteButton.getInitWidth()/2,
            noteButton.getInitY() + noteButton.getInitHeight()/2 + LABEL_OFFSET_Y,

            ((noteButton.isPlaying() && !foreignPlaying)
                ? themeLoader.labelPressed(noteButton)
                : themeLoader.labelReleased(noteButton)
            ).getRGB()
        );
    }


    protected void renderAccidentals(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        RenderSystem.enableBlend();
        
        switch (noteButton.getNotation()) {
            case NONE: break;

            case FLAT:
                renderAccidental(gui, 0);
                break;
            case SHARP:
                renderAccidental(gui, 1);
                break;
            case DOUBLE_FLAT:
                renderAccidental(gui, 0, -6, -3);
                renderAccidental(gui, 0, 5, 2);
                break;
            case DOUBLE_SHARP:
                renderAccidental(gui, 2, -1, 0);
                break;

        }
    }
    
    protected void renderAccidental(final GuiGraphics gui, int index) {
        renderAccidental(gui, index, 0, 0);
    }
    protected void renderAccidental(GuiGraphics gui, int index, int offsetX, int offsetY) {
        final double textureMultiplier = noteButton.getWidth() * (
            // Handle sharp size
            (index == 1) ? SHARP_MULTIPLIER : 1
        ) * 2;

        final int textureWidth = (int)(textureMultiplier),
            textureHeight = (int)(textureMultiplier) - 1;

        final int spritePartWidth = textureWidth/3 + 1;


        gui.blit(accidentalsLocation,
            noteButton.getX() - 9 + offsetX, noteButton.getY() - 5 + offsetY,
            spritePartWidth * index, noteButton.isPlaying() ? (textureHeight + 1)/2 : 0,
            
            spritePartWidth - 1, textureHeight/2,
            textureWidth, textureHeight
        );
    }


    public void playNoteAnimation(final boolean isForeign) {
        foreignPlaying = isForeign;

        noteAnimation.play(isForeign);
        addRing();
    }
    public void addRing() {
        final NoteRing ring = new NoteRing(noteButton, foreignPlaying);
        rings.add(ring);
        ring.playAnim();
    }

    public void resetAnimations() {
        rings.clear();
        noteAnimation.stop();
    }


    /**
     * Obtains a resource from this instrument's directory.
     * @param path The resource to obtain from this note's directory
     * @see CommonUtil#getResourceFrom(ResourceLocation, String)
     */
    protected ResourceLocation getResourceFromRoot(final String path) {
        return CommonUtil.getResourceFrom(rootLocation, path);
    }

}
