package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.SpeakerBlock;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.block.blockentity.speaker.SpeakerAddress;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * a speaker block. the relaying and pairing itself is handled by its {@link #speaker()}
 */
public class SpeakerBlockEntity extends BlockEntity implements Speaker.Host {
    private final Speaker speaker;

    public SpeakerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SPEAKER.get(), pPos, pBlockState);
        this.speaker = new Speaker(this, new InitiatorID("block",
            String.format("x%sy%sz%s", pPos.getX(), pPos.getY(), pPos.getZ())
        ));
    }

    public Speaker speaker() {
        return speaker;
    }

    //#region Speaker.Host implementation

    @Override
    public BlockPos getSoundPos() {
        return getBlockPos();
    }
    @Override
    public SpeakerAddress getAddress() {
        return new SpeakerAddress.AtBlock(getBlockPos());
    }

    /**
     * swaps da front texture between speaker_front and speaker_front_connected
     */
    @Override
    public boolean isConnected() {
        return getBlockState().getValue(SpeakerBlock.CONNECTED);
    }
    @Override
    public void setConnected(final boolean connected) {
        if (level != null)
            level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(SpeakerBlock.CONNECTED, connected));
    }

    //#endregion

    public void tick(final Level level) {
        speaker.tick(level);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        speaker.save(pTag);
    }
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        speaker.load(pTag);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        speaker.releaseSustainedNotes();
    }
}
