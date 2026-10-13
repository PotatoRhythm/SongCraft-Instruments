package com.stump.songcraft_instruments.capability.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

public class RecordingCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<RecordingCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private RecordingCapability cap;
    private final LazyOptional<RecordingCapability> optional = LazyOptional.of(this::getInstance);
    private RecordingCapability getInstance() {
        return (cap == null) ? (cap = new RecordingCapability()) : cap;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        getInstance().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getInstance().loadNBTData(nbt);
    }


    public static void setConnection(final Player player, final BlockPos looperPos, final UUID connectionId) {
        player.getCapability(CAPABILITY).ifPresent((cap) -> cap.setConnection(looperPos, connectionId));
    }
    public static BlockPos getConnectedLooperPos(final Player player) {
        return getProp(player, RecordingCapability::getConnectedLooperPos, null);
    }
    public static UUID getConnectionId(final Player player) {
        return getProp(player, RecordingCapability::getConnectionId, null);
    }

    public static void setParticleColors(final Player player, final int[] colors) {
        player.getCapability(CAPABILITY).ifPresent((cap) -> cap.setParticleColors(colors));
    }
    public static int[] getParticleColors(final Player player) {
        return getProp(player, RecordingCapability::getParticleColors, new int[0]);
    }

    public static void setVolume(final Player player, final int volume) {
        player.getCapability(CAPABILITY).ifPresent((cap) -> cap.setVolume(volume));
    }
    public static int getVolume(final Player player) {
        return getProp(player, RecordingCapability::getVolume, -1);
    }


    private static <T> T getProp(Player player, Function<RecordingCapability, T> ifExists, T elseVal) {
        final LazyOptional<RecordingCapability> lazyOpen = player.getCapability(RecordingCapabilityProvider.CAPABILITY);

        return lazyOpen.isPresent()
            ? ifExists.apply(lazyOpen.resolve().get())
            : elseVal;
    }
}
