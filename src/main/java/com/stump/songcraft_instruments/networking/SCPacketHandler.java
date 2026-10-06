package com.stump.songcraft_instruments.networking;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.networking.packet.instrument.c2s.*;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.*;
import com.stump.songcraft_instruments.networking.packet.*;
import com.stump.songcraft_instruments.networking.packet.record.*;
import com.stump.songcraft_instruments.util.ServerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.List;

@EventBusSubscriber(modid = SCInstrumentMod.MODID, bus = Bus.MOD)
public class SCPacketHandler {
    @SuppressWarnings("unchecked")
    public static final List<Class<IModPacket>> ACCEPTABLE_PACKETS = List.of(new Class[] {
        NotifyInstrumentOpenPacket.class,
        C2SNoteSoundPacket.class, S2CNoteSoundPacket.class,
        C2SDampenNotesPacket.class, S2CDampenNotesPacket.class,
        OpenInstrumentPacket.class, CloseInstrumentPacket.class,
        C2SHeldNoteSoundPacket.class, S2CHeldNoteSoundPacket.class,
        LooperRecordStatePacket.class, OpenNoteBlockInstrumentPacket.class,
        S2CLooperParticlePacket.class, C2SColorSetAcceptPacket.class, C2SActiveColorSetPacket.class,
        S2CColorSetAddPacket.class, S2CColorSetConfirmationPacket.class,
        S2CLooperDampenPacket.class,
        // Sync stuff
        DoesLooperExistPacket.class, LooperUnplayablePacket.class, SyncModTagPacket.class,
        LooperPlayStatePacket.class, LooperConnectionsPacket.class, LooperRestartPacket.class,
        // Record files
        S2CRecordFilePartPacket.class, C2SRecordFilePartPacket.class,
        S2CRecordImportRequestPacket.class, S2CRecordFilesListPacket.class
    });

    private static int id = 0;
    public static void registerPackets() {
        ServerUtil.registerModPackets(INSTANCE, ACCEPTABLE_PACKETS, () -> id++);
    }


    private static final String PROTOCOL_VERSION = "1.1.0";

    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SCInstrumentMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );


    public static <T> void sendToServer(final T packet) {
        INSTANCE.sendToServer(packet);
    }
    public static <T> void sendToClient(final T packet, final ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static <T> void sendToTracking(T packet, ServerLevel level, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
    }
    public static <T> void sendToTrackingEntity(final T packet, final ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
    }
    public static <T> void sendToTrackingEntityAndSelf(final T packet, final ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
}
