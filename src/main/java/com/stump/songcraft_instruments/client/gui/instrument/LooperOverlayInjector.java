package com.stump.songcraft_instruments.client.gui.instrument;

import com.stump.songcraft_instruments.block.blockentity.looper.RecordingSession;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.colorSet.ColorSetManager;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings; //hmm
import com.stump.songcraft_instruments.mixins.required.ScreenAccessor;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.block.util.LooperSessionState;
import com.stump.songcraft_instruments.networking.packet.DoesLooperExistPacket;
import com.stump.songcraft_instruments.networking.packet.LooperConnectionsPacket;
import com.stump.songcraft_instruments.networking.packet.LooperRecordStatePacket;
import com.stump.songcraft_instruments.networking.packet.LooperRestartPacket;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class LooperOverlayInjector {
    private static final int REC_BTN_WIDTH = 120, REC_BTN_Y = 5;
    private static final int RESTART_BTN_SIZE = 20, RESTART_BTN_GAP = 4;
    // A counter-clockwise arrow, centered in the restart button
    private static final ResourceLocation RESTART_ICON =
        new ResourceLocation(SCInstrumentMod.MODID, "textures/gui/sprites/restart.png");
    private static final int RESTART_ICON_SIZE = 16;
    // Placed below the instrument screen's visibility button
    private static final int CONNECTIONS_X = 6, CONNECTIONS_Y = 32, CONNECTIONS_LINE_HEIGHT = 10;
    private static final int
        CONNECTION_COLOR = 0xFFFFFF,
        CONNECTION_SELF_COLOR = 0xFFFF55,
        CONNECTION_OFFLINE_COLOR = 0x808080
    ;

    private static InstrumentScreen screen = null;
    private static boolean isRecording = false;
    private static Button recordBtn;
    private static Button restartBtn;

    // The looper the current screen is showing, and its synced state
    private static BlockPos looperPos = null;
    private static List<LooperConnectionsPacket.Entry> connections = List.of();
    private static LooperSessionState sessionState = LooperSessionState.IDLE;

    /**
     * The looper whose group session we are participating in, if any.
     * Tracked even with no instrument open, as participants are recorded on any instrument they open.
     */
    private static BlockPos groupLooperPos = null;
    /**
     * Whether the current screen's instrument is itself connected to {@link #looperPos},
     * as opposed to only showing it for being in a group session
     */
    private static boolean screenInstrumentConnected = false;

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InstrumentScreen instrumentScreen))
            return;

        final Player player = Minecraft.getInstance().player;
        final boolean isItem = InstrumentOpenProvider.isItem(player);

        // The looper this instrument is connected to, if any
        BlockPos instrumentLooperPos = null;
        if (isItem) {
            final ItemStack instrumentItem = player.getItemInHand(InstrumentOpenProvider.getHand(player));
            if (LooperUtil.hasLooperTag(instrumentItem))
                instrumentLooperPos = LooperUtil.getLooperPos(LooperUtil.looperTag(instrumentItem));
        } else {
            final BlockEntity instrumentBE = player.level().getBlockEntity(InstrumentOpenProvider.getBlockPos(player));
            if (instrumentBE != null && LooperUtil.hasLooperTag(instrumentBE))
                instrumentLooperPos = LooperUtil.getLooperPos(LooperUtil.looperTag(instrumentBE));
        }

        // Group participants get the looper's controls on any instrument
        if (instrumentLooperPos == null && groupLooperPos == null)
            return;

        looperPos = (groupLooperPos != null) ? groupLooperPos : instrumentLooperPos;
        screenInstrumentConnected = looperPos.equals(instrumentLooperPos);

        SCPacketHandler.sendToServer(isItem
            ? new DoesLooperExistPacket(InstrumentOpenProvider.getHand(player))
            : new DoesLooperExistPacket()
        );

        LooperOverlayInjector.screen = instrumentScreen;
        if (groupLooperPos != null) {
            // Keep the session state we already know of until the server responds
            isRecording = sessionState != LooperSessionState.IDLE;
        } else {
            connections = List.of();
            sessionState = LooperSessionState.IDLE;
        }

        event.addListener(
            recordBtn = Button.builder(
                    appendRecordKeyHint(Component.translatable("button.songcraft_instruments.record")),
                    LooperOverlayInjector::onRecordPress
                )
                .width(REC_BTN_WIDTH)
                .pos((instrumentScreen.width - REC_BTN_WIDTH) / 2, REC_BTN_Y)
                .build()
        );
        event.addListener(
            restartBtn = createRestartButton()
        );
        updateRecordButtonLabel();
    }

    private static Button createRestartButton() {
        final Button button = new Button(0, 0, RESTART_BTN_SIZE, RESTART_BTN_SIZE,
                Component.translatable("button.songcraft_instruments.restart"),
                LooperOverlayInjector::onRestartPress, Supplier::get) {

            // Draw the icon in place of the button's text
            @Override
            public void renderString(GuiGraphics gui, Font font, int color) {
                final int margin = (RESTART_BTN_SIZE - RESTART_ICON_SIZE) / 2;
                gui.blit(RESTART_ICON, getX() + margin, getY() + margin, 0, 0,
                    RESTART_ICON_SIZE, RESTART_ICON_SIZE, RESTART_ICON_SIZE, RESTART_ICON_SIZE
                );
            }
        };

        button.setTooltip(Tooltip.create(Component.translatable("button.songcraft_instruments.restart")));
        return button;
    }

    private static void onRestartPress(final Button btn) {
        // The recording starts over; credit the player with their current particle colors
        ColorSetManager.sendActiveSetToServer();
        SCPacketHandler.sendToServer(new LooperRestartPacket());
    }

    /**
     * The record button is always centered. The restart button shows alongside the stop button, to its right.
     */
    private static void updateButtonLayout() {
        if (screen == null || recordBtn == null || restartBtn == null)
            return;

        restartBtn.visible = isRecording;

        final int x = (screen.width - REC_BTN_WIDTH) / 2;

        recordBtn.setPosition(x, REC_BTN_Y);
        restartBtn.setPosition(x + REC_BTN_WIDTH + RESTART_BTN_GAP, REC_BTN_Y);
    }
    public static void handleLooperRemoved() {
        removeRecordButton();
        screen = null;
        connections = List.of();
        // The server only reports a looper as unplayable when we aren't in a group session
        groupLooperPos = null;
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        handleLooperRemoved();
        looperPos = null;
        sessionState = LooperSessionState.IDLE;
        isRecording = false;
    }

    @SuppressWarnings("resource")
    public static void handleConnectionsSync(final BlockPos syncedLooperPos,
                                             final List<LooperConnectionsPacket.Entry> syncedConnections,
                                             final LooperSessionState syncedState,
                                             final boolean syncedGroupSession) {
        final UUID selfId = Minecraft.getInstance().player.getUUID();
        final boolean selfConnected = syncedConnections.stream().anyMatch((entry) -> entry.playerId().equals(selfId));
        final boolean wasGroupSession = syncedLooperPos.equals(groupLooperPos);

        // A group recording we participate in is starting; credit us with our current particle colors
        if (selfConnected && syncedGroupSession && (syncedState == LooperSessionState.COUNTDOWN))
            ColorSetManager.sendActiveSetToServer();

        // Track the group session we participate in, even with no instrument open
        if (selfConnected && syncedGroupSession)
            groupLooperPos = syncedLooperPos;
        else if (syncedLooperPos.equals(groupLooperPos))
            groupLooperPos = null;

        if (syncedLooperPos.equals(groupLooperPos) || syncedLooperPos.equals(looperPos)) {
            connections = syncedConnections;
            sessionState = syncedState;
        }

        if (screen == null || !syncedLooperPos.equals(looperPos))
            return;

        // We were disconnected from this looper,
        // or its group session ended and this instrument was only showing it for being a participant
        if (!selfConnected || (syncedState == LooperSessionState.IDLE && !screenInstrumentConnected)) {
            handleLooperRemoved();
            return;
        }

        // The server is the source of truth for group sessions, as any participant may start or stop them.
        // This includes a session cancelled down to a single player, who goes back to an idle solo looper.
        if (isGroup() || wasGroupSession)
            isRecording = syncedState != LooperSessionState.IDLE;

        updateRecordButtonLabel();
    }

    @Nullable
    private static Component getSessionStatus() {
        return switch (sessionState) {
            case COUNTDOWN -> Component.translatable("gui.songcraft_instruments.looper.state.countdown")
                .withStyle(ChatFormatting.GOLD);
            case ARMED -> Component.translatable("gui.songcraft_instruments.looper.state.armed")
                .withStyle(ChatFormatting.GREEN);
            case RECORDING -> Component.translatable("gui.songcraft_instruments.looper.state.recording")
                .withStyle(ChatFormatting.RED);
            default -> null;
        };
    }

    /**
     * @return The players shown as connected. Before a group recording starts,
     * only players on their connected instrument's screen are taking part.
     */
    private static List<LooperConnectionsPacket.Entry> activeConnections() {
        if (sessionState != LooperSessionState.IDLE)
            return connections;

        return connections.stream().filter(LooperConnectionsPacket.Entry::present).toList();
    }

    private static boolean isGroup() {
        return activeConnections().size() > 1 || (looperPos != null && looperPos.equals(groupLooperPos));
    }

    private static void updateRecordButtonLabel() {
        if (recordBtn == null)
            return;

        final String key;
        if (isRecording)
            key = "button.songcraft_instruments.stop";
        else if (isGroup())
            key = "button.songcraft_instruments.group_record";
        else
            key = "button.songcraft_instruments.record";

        recordBtn.setMessage(appendRecordKeyHint(Component.translatable(key)));
        updateButtonLayout();
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onScreenRender(final ScreenEvent.Render.Post event) {
        if (event.getScreen() != screen || recordBtn == null || !isGroup() || !screen.instrumentRenders())
            return;

        final GuiGraphics gui = event.getGuiGraphics();
        final Font font = Minecraft.getInstance().font;
        final UUID selfId = Minecraft.getInstance().player.getUUID();

        int y = CONNECTIONS_Y;

        Component status = getSessionStatus();
        // Participants out of range are not recorded
        if (looperPos.equals(groupLooperPos) && !looperPos.closerToCenterThan(Minecraft.getInstance().player.position(), RecordingSession.MAX_RECORD_DIST))
            status = Component.translatable("gui.songcraft_instruments.looper.out_of_range").withStyle(ChatFormatting.RED);

        if (status != null) {
            gui.drawString(font, status, CONNECTIONS_X, y, CONNECTION_COLOR);
            y += CONNECTIONS_LINE_HEIGHT + 2;
        }

        final List<LooperConnectionsPacket.Entry> shownConnections = activeConnections();
        gui.drawString(font,
            Component.translatable("gui.songcraft_instruments.looper.connected", shownConnections.size()),
            CONNECTIONS_X, y, CONNECTION_COLOR
        );

        for (final LooperConnectionsPacket.Entry entry : shownConnections) {
            y += CONNECTIONS_LINE_HEIGHT;

            final int color;
            if (!entry.online())
                color = CONNECTION_OFFLINE_COLOR;
            else if (entry.playerId().equals(selfId))
                color = CONNECTION_SELF_COLOR;
            else
                color = CONNECTION_COLOR;

            gui.drawString(font, "- " + entry.playerName(), CONNECTIONS_X, y, color);
        }
    }

    private static MutableComponent appendRecordKeyHint(final MutableComponent component) {
        return component
            .append(" (")
            .append(InstrumentKeyMappings.RECORD.get().getKey().getDisplayName())
            .append(")");
    }

    @SubscribeEvent
    public static void onKeyboardPress(final ScreenEvent.KeyPressed.Pre event) {
        if (InstrumentKeyMappings.RECORD.get().matches(event.getKeyCode(), event.getScanCode())) {

            if (recordBtn != null) {
                recordBtn.playDownSound(Minecraft.getInstance().getSoundManager());
                recordBtn.onPress();
            }

        }
    }

    @SubscribeEvent
    public static void onScreenClose(final ScreenEvent.Closing event) {
        if (event.getScreen() != screen)
            return;

        isRecording = false;
        LooperOverlayInjector.screen = null;
        // Keep the group session's state, to be shown on the next instrument opened
        if (groupLooperPos == null)
            connections = List.of();
    }

    @SuppressWarnings("resource")
    private static void onRecordPress(final Button btn) {
        final LocalPlayer player = Minecraft.getInstance().player;

        final boolean isItem = InstrumentOpenProvider.isItem(player);
        final InteractionHand hand = isItem ?
            InstrumentOpenProvider.getHand(Minecraft.getInstance().player)
            : null;

        final boolean wasRecording = isRecording;
        isRecording = !isRecording;

        // Group participants keep their button until the server syncs the session's end
        if (wasRecording && !isGroup()) {
            removeRecordButton();
            screen = null;
        } else
            updateRecordButtonLabel();
        // A recording is starting; credit the player with their current particle colors
        if (isRecording)
            ColorSetManager.sendActiveSetToServer();
        SCPacketHandler.sendToServer(new LooperRecordStatePacket(isRecording, hand));
    }

//    private static BlockEntity getIBE(final Player player) {
//        final BlockPos instrumentPos = InstrumentOpenProvider.getBlockPos(player);
//
//        return (instrumentPos == null) ? null
//            : player.level().getBlockEntity(instrumentPos);
//    }


    /**
     * @return The looper buttons currently on the provided screen, for it to add back after clearing its widgets
     */
    public static List<Button> getButtons(final Screen targetScreen) {
        if (targetScreen != screen)
            return List.of();

        return Stream.of(recordBtn, restartBtn).filter(Objects::nonNull).toList();
    }

    public static void removeRecordButton() {
        if (screen == null)
            return;

        ((ScreenAccessor)screen).invokeRemoveWidget(recordBtn);
        recordBtn = null;

        if (restartBtn != null) {
            ((ScreenAccessor)screen).invokeRemoveWidget(restartBtn);
            restartBtn = null;
        }
    }
}
