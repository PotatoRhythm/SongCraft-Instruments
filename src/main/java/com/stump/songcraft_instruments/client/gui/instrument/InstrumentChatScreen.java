package com.stump.songcraft_instruments.client.gui.instrument;

import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.mixins.required.ChatScreenAccessor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * The vanilla chat, opened as a layer on top of an instrument screen.
 * Sending a message or pressing escape returns to the instrument instead of closing it,
 * so the instrument stays open the whole time.
 */
@OnlyIn(Dist.CLIENT)
public class InstrumentChatScreen extends ChatScreen {
    public final InstrumentScreen instrumentScreen;

    public InstrumentChatScreen(final InstrumentScreen instrumentScreen) {
        super("");
        this.instrumentScreen = instrumentScreen;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        final boolean isEnter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;

        // Vanilla closes every screen on these, which would close the instrument too
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || isEnter) {
            // Let the command suggestions consume them first (e.g. escape hiding the suggestions)
            if (((ChatScreenAccessor) this).getCommandSuggestions().keyPressed(keyCode, scanCode, modifiers))
                return true;

            if (!isEnter || handleChatInput(input.getValue(), true))
                returnToInstrument();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        returnToInstrument();
    }

    private void returnToInstrument() {
        minecraft.popGuiLayer();
    }
}
