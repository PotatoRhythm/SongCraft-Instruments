package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.menu.PedalMenu;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.pedal.PedalInput;
import com.stump.songcraft_instruments.pedal.PedalLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * An effects pedal: its effect's knobs, whether it is bypassed, and what it is plugged into.
 * <p>Pedals chain into each other and end at a speaker block: instrument → pedal → pedal → speaker.
 * The speaker caches the resulting {@link com.stump.songcraft_instruments.pedal.PedalChain} and sends it along with
 * the notes it relays, so a pedal only has to tell its speaker whenever it changes (see {@link PedalLinks}).</p>
 */
public class PedalBlockEntity extends BlockEntity implements PedalInput, MenuProvider {
    private static final String KNOBS_TAG = "knobs", INPUT_TAG = "input", OUTPUT_TAG = "output";

    /**
     * The menu's data: one slot per knob, then the bypass state
     */
    public static final int DATA_BYPASSED = PedalEffectType.MAX_KNOBS, DATA_COUNT = DATA_BYPASSED + 1;

    private PedalEffect effect;
    /**
     * The pedal plugged into this one, or null
     */
    @Nullable
    private BlockPos input;
    /**
     * What this pedal is plugged into: the next pedal or a speaker block, or null
     */
    @Nullable
    private BlockPos output;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(final int index) {
            if (index < PedalEffectType.MAX_KNOBS)
                return effect.knob(index);
            if (index == DATA_BYPASSED)
                return isBypassed() ? 1 : 0;
            return 0;
        }
        // Only ever changed through PedalMenu's buttons
        @Override
        public void set(final int index, final int value) {}
        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PedalBlockEntity(final BlockPos pPos, final BlockState pBlockState) {
        super(ModBlockEntities.PEDAL.get(), pPos, pBlockState);
        this.effect = typeOf(pBlockState).defaultEffect();
    }

    private static PedalEffectType typeOf(final BlockState state) {
        return (state.getBlock() instanceof PedalBlock pedal) ? pedal.effectType : PedalEffectType.FUZZ;
    }

    public PedalEffect getEffect() {
        return effect;
    }
    public boolean isBypassed() {
        return !getBlockState().getValue(PedalBlock.ENABLED);
    }

    /**
     * Server only. Turns a knob by the given amount of steps
     */
    public void stepKnob(final int knob, final int steps) {
        if (knob < 0 || knob >= effect.type().knobCount())
            return;

        final PedalEffect turned = effect.withKnob(knob, effect.knob(knob) + steps);
        if (turned.equals(effect))
            return;

        effect = turned;
        onEffectChanged();
    }
    /**
     * Server only. Stomps the pedal: switches its effect on or off
     */
    public void toggleBypassed() {
        if (level == null)
            return;

        level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(PedalBlock.ENABLED, isBypassed()));
        onEffectChanged();
    }

    private void onEffectChanged() {
        setChanged();
        if (level != null)
            PedalLinks.notifySpeaker(level, getBlockPos());
    }


    //#region Links

    @Override
    public @Nullable BlockPos getPedalInput() {
        return input;
    }
    @Override
    public void setPedalInput(@Nullable final BlockPos pedalPos) {
        input = pedalPos;
        setChanged();
    }

    @Nullable
    public BlockPos getOutput() {
        return output;
    }
    public void setOutput(@Nullable final BlockPos target) {
        output = target;
        setChanged();
    }

    //#endregion


    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new PedalMenu(containerId, this, menuData);
    }


    @Override
    protected void saveAdditional(final CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt(KNOBS_TAG, effect.knobs());
        if (input != null)
            pTag.put(INPUT_TAG, NbtUtils.writeBlockPos(input));
        if (output != null)
            pTag.put(OUTPUT_TAG, NbtUtils.writeBlockPos(output));
    }
    @Override
    public void load(final CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains(KNOBS_TAG, Tag.TAG_INT))
            effect = new PedalEffect(typeOf(getBlockState()), pTag.getInt(KNOBS_TAG));
        input = pTag.contains(INPUT_TAG, Tag.TAG_COMPOUND) ? NbtUtils.readBlockPos(pTag.getCompound(INPUT_TAG)) : null;
        output = pTag.contains(OUTPUT_TAG, Tag.TAG_COMPOUND) ? NbtUtils.readBlockPos(pTag.getCompound(OUTPUT_TAG)) : null;
    }
}
