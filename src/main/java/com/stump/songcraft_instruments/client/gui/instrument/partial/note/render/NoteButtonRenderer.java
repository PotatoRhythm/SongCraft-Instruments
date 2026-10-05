package com.stump.songcraft_instruments.client.gui.instrument.partial.note.render;

import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.animation.NoteAnimationController;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    /** The buttons used by instruments that have none of their own */
    protected static final ResourceLocation DEFAULT_NOTE_ROOT =
        InstrumentScreen.getInternalResourceFromGlob("instrument/windsong_lyre/note");

    // Guild Wars 2 style; one button per note letter
    protected static final ResourceLocation GW2_NOTE_ROOT = new ResourceLocation(
            "songcraft_instruments",
            "textures/gui/songcraft_instruments/instrument/gw2"
    );
    private static final String[] GW2_NOTE_LETTERS = {"c", "d", "e", "f", "g", "a", "b"};
    protected ResourceLocation gw2AccidentalsLocation;
    protected ResourceLocation gw2PressedLocation, gw2ReleasedLocation, gw2HoverLocation;

    /** The space between the bottom of a low-octave Jianpu dot and the top of the label, in screen pixels */
    protected static final int LABEL_GAP = 2;

    // Genshin and Jianpu styles; drawn 1:1 on the button's texture pixel grid
    /** The size of the default button texture, which defines the pixel grid */
    public static final int BUTTON_TEXTURE_SIZE = 16;
    public static final int JIANPU_SYMBOL_WIDTH = 7, JIANPU_SYMBOL_HEIGHT = 9;
    /** How much larger than the button's pixel grid the Jianpu symbol and its octave dot are drawn */
    protected static final float JIANPU_SYMBOL_SCALE = 1.2f;
    /** How far the octave dot is moved towards the number from its texture row, in symbol texture pixels */
    protected static final float JIANPU_DOT_INSET = 0.5f;
    /** A single pixel, drawn above or below a Jianpu symbol to mark its octave */
    protected static final ResourceLocation OCTAVE_DOT_LOCATION =
        InstrumentScreen.getInternalResourceFromGlob("note/label/grid_generic/dot.png");

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
        // Guild Wars 2 instruments only come with GW2 buttons
        if (!resourceExists(getResourceFromRoot("note/released.png")))
            rootLocation = DEFAULT_NOTE_ROOT;

        accidentalsLocation = getResourceFromRoot("accidentals.png");
        noteReleasedLocation = getResourceFromRoot("note/released.png");
        notePressedLocation = getResourceFromRoot("note/pressed.png");
        noteHoverLocation = getResourceFromRoot("note/hovered.png");

        // Every note has both, as the note style may change while the instrument is open
        final String noteLetter = GW2_NOTE_LETTERS[noteButton.soundIndex() % GW2_NOTE_LETTERS.length];
        gw2AccidentalsLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "accidentals_" + noteLetter + ".png");
        gw2ReleasedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "released_" + noteLetter + ".png");
        gw2PressedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "pressed_" + noteLetter + ".png");
        gw2HoverLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "hovered_" + noteLetter + ".png");
    }

    protected static boolean resourceExists(final ResourceLocation location) {
        return MINECRAFT.getResourceManager().getResource(location).isPresent();
    }

    protected ResourceLocation getNotePressedLocation() {
        return noteButton.usesGw2Buttons() ? gw2PressedLocation : notePressedLocation;
    }
    protected ResourceLocation getNoteReleasedLocation() {
        return noteButton.usesGw2Buttons() ? gw2ReleasedLocation : noteReleasedLocation;
    }
    protected ResourceLocation getNoteHoverLocation() {
        return noteButton.usesGw2Buttons() ? gw2HoverLocation : noteHoverLocation;
    }
    protected ResourceLocation getAccidentalsLocation() {
        return noteButton.usesGw2Buttons() ? gw2AccidentalsLocation : accidentalsLocation;
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

        renderPixelGridNoteButton(gui, noteLocation);
    }

    protected void renderNoteSymbol(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        // GW2 buttons have their note letters built in
        if (noteButton.usesGw2Buttons())
            return;

        final int noteWidth = noteButton.getWidth()/2, noteHeight = noteButton.getHeight()/2;

        ClientUtil.setShaderColor((noteButton.isPlaying() && !foreignPlaying)
            ? themeLoader.notePressed(noteButton)
            : themeLoader.noteReleased(noteButton)
        );

        if (noteButton.usesPixelGridSymbol()) {
            if (noteButton.usesJianpuSymbol())
                renderJianpuSymbol(gui);
            else
                renderCenteredSymbol(gui);

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

        ClientUtil.resetShaderColor();
    }

    /**
     * @return The row of the symbol's texture that its octave dot sits on,
     * moved {@link #JIANPU_DOT_INSET} towards the number
     */
    protected float getOctaveDotRow() {
        return (noteButton.getOctaveDot() > 0)
            ? JIANPU_DOT_INSET
            : (JIANPU_SYMBOL_HEIGHT - 1 - JIANPU_DOT_INSET);
    }

    /**
     * Draws the button 1:1 on the pixel grid, centered.
     * 16x16 buttons fill it, as the drums' and the GW2 buttons do; the grid instruments' are 15x15,
     * so that the 1-pixel-wide symbols have a true center.
     */
    protected void renderPixelGridNoteButton(final GuiGraphics gui, final ResourceLocation noteLocation) {
        final TextureBounds bounds = getTextureBounds(noteLocation);
        final int width = (bounds == null) ? BUTTON_TEXTURE_SIZE : bounds.width(),
            height = (bounds == null) ? BUTTON_TEXTURE_SIZE : bounds.height();

        blitOnPixelGrid(gui, noteLocation,
            (BUTTON_TEXTURE_SIZE - width) / 2f, (BUTTON_TEXTURE_SIZE - height) / 2f,
            width, height
        );
    }

    protected void renderJianpuSymbol(final GuiGraphics gui) {
        // Scale around the button's center, so the symbol stays exactly centered
        final float center = BUTTON_TEXTURE_SIZE / 2f;
        final float x = center - JIANPU_SYMBOL_WIDTH * JIANPU_SYMBOL_SCALE / 2;
        final float y = center - JIANPU_SYMBOL_HEIGHT * JIANPU_SYMBOL_SCALE / 2
            + noteButton.getPixelGridSymbolOffsetY();

        blitOnPixelGrid(gui, labelTextureProvider.get(), x, y,
            JIANPU_SYMBOL_WIDTH, JIANPU_SYMBOL_HEIGHT, JIANPU_SYMBOL_SCALE);

        if (noteButton.getOctaveDot() != 0) {
            blitOnPixelGrid(gui, OCTAVE_DOT_LOCATION,
                x + (JIANPU_SYMBOL_WIDTH / 2) * JIANPU_SYMBOL_SCALE, y + getOctaveDotRow() * JIANPU_SYMBOL_SCALE,
                1, 1, JIANPU_SYMBOL_SCALE);
        }
    }

    /**
     * Draws the symbol 1:1 on the button's pixel grid, with its visible pixels centered on the button.
     * Used by the Genshin symbols, whose textures leave different amounts of empty space below them.
     */
    protected void renderCenteredSymbol(final GuiGraphics gui) {
        final ResourceLocation texture = labelTextureProvider.get();
        final TextureBounds bounds = getTextureBounds(texture);
        if (bounds == null)
            return;

        final float center = BUTTON_TEXTURE_SIZE / 2f;
        final float x = center - (bounds.left + bounds.right) / 2f,
            y = center - (bounds.top + bounds.bottom) / 2f;

        blitOnPixelGrid(gui, texture, x, y, bounds.width, bounds.height);
    }

    /**
     * The size of a texture, and the edges of its visible pixels.
     * {@code right} and {@code bottom} are exclusive.
     */
    protected record TextureBounds(int width, int height, int left, int top, int right, int bottom) {}
    private static final Map<ResourceLocation, Optional<TextureBounds>> TEXTURE_BOUNDS = new HashMap<>();

    /**
     * Measures the size and visible pixels of a texture once, and remembers them
     * @return The texture's bounds, or null if it could not be read
     */
    protected static @Nullable TextureBounds getTextureBounds(final ResourceLocation texture) {
        return TEXTURE_BOUNDS.computeIfAbsent(texture, NoteButtonRenderer::measureTexture).orElse(null);
    }
    private static Optional<TextureBounds> measureTexture(final ResourceLocation texture) {
        try (InputStream stream = MINECRAFT.getResourceManager().open(texture);
             NativeImage image = NativeImage.read(stream)) {

            final int width = image.getWidth(), height = image.getHeight();
            int left = width, top = height, right = 0, bottom = 0;

            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++) {
                    if ((image.getPixelRGBA(x, y) >>> 24) == 0)
                        continue;

                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x + 1);
                    bottom = Math.max(bottom, y + 1);
                }

            // Fully transparent; center the whole texture
            if (right == 0)
                return Optional.of(new TextureBounds(width, height, 0, 0, width, height));

            return Optional.of(new TextureBounds(width, height, left, top, right, bottom));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Draws a texture at the scale of the button's {@link #BUTTON_TEXTURE_SIZE 16x16} pixel grid,
     * so each texture pixel covers exactly one button pixel.
     * @param pixelX The horizontal position, in button pixels
     * @param pixelY The vertical position, in button pixels
     */
    protected void blitOnPixelGrid(final GuiGraphics gui, final ResourceLocation texture,
            final float pixelX, final float pixelY, final int textureWidth, final int textureHeight) {
        blitOnPixelGrid(gui, texture, pixelX, pixelY, textureWidth, textureHeight, 1);
    }
    /**
     * Draws a texture at a multiple of the button's pixel grid
     * @param scale How many button pixels each texture pixel covers
     * @see #blitOnPixelGrid(GuiGraphics, ResourceLocation, float, float, int, int)
     */
    protected void blitOnPixelGrid(final GuiGraphics gui, final ResourceLocation texture,
            final float pixelX, final float pixelY, final int textureWidth, final int textureHeight, final float scale) {
        final float pixelWidth = noteButton.getWidth() / (float) BUTTON_TEXTURE_SIZE * scale,
            pixelHeight = noteButton.getHeight() / (float) BUTTON_TEXTURE_SIZE * scale;
        final float gridPixelWidth = noteButton.getWidth() / (float) BUTTON_TEXTURE_SIZE,
            gridPixelHeight = noteButton.getHeight() / (float) BUTTON_TEXTURE_SIZE;

        gui.pose().pushPose();
        gui.pose().translate(noteButton.getX() + pixelX * gridPixelWidth, noteButton.getY() + pixelY * gridPixelHeight, 0);
        gui.pose().scale(pixelWidth, pixelHeight, 1);

        gui.blit(texture,
            0, 0,
            0, 0,

            textureWidth, textureHeight,
            textureWidth, textureHeight
        );

        gui.pose().popPose();
    }

    /**
     * Places the label below a low-octave Jianpu dot, so the two never overlap.
     * Used for every note style, so the label stays put when switching between them.
     * @return How far below the button's center the top of the label is drawn, in GUI units
     */
    protected float getLabelOffsetY() {
        // The Jianpu symbol is centered on the button, and its low dot sits on its bottom row
        final float pixelHeight = noteButton.getInitHeight() / (float) BUTTON_TEXTURE_SIZE;
        final float dotBottom = (JIANPU_SYMBOL_HEIGHT / 2f - JIANPU_DOT_INSET) * JIANPU_SYMBOL_SCALE * pixelHeight;

        return dotBottom + LABEL_GAP / (float) MINECRAFT.getWindow().getGuiScale();
    }

    protected void renderLabel(final GuiGraphics gui, final InstrumentThemeLoader themeLoader) {
        // Positioned in between GUI units, so the gap below the dot can be a single screen pixel
        gui.pose().pushPose();
        gui.pose().translate(0, noteButton.getInitY() + noteButton.getInitHeight() / 2f + getLabelOffsetY(), 0);

        gui.drawCenteredString(
            MINECRAFT.font, noteButton.getMessage(),
            noteButton.getInitX() + noteButton.getInitWidth()/2, 0,

            ((noteButton.isPlaying() && !foreignPlaying)
                ? themeLoader.labelPressed(noteButton)
                : themeLoader.labelReleased(noteButton)
            ).getRGB()
        );

        gui.pose().popPose();
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


        gui.blit(getAccidentalsLocation(),
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
