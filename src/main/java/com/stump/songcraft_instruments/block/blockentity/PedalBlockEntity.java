package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.menu.PedalMenu;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.pedal.PedalLinks;
import com.stump.songcraft_instruments.pedal.PedalMonitorSync;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An effects pedal: its effect's knobs, whether it is bypassed, and the pedals it is cabled to.
 * <p>Instruments plug into a pedal, and play through it and every pedal after it:
 * instrument → pedal → pedal. Several pedals may feed into the same one, so a band can share
 * a pedal at the end of their chains (see {@link PedalLinks}).</p>
 */
public class PedalBlockEntity extends BlockEntity implements MenuProvider {
    private static final String ID_TAG = "pedal_id", KNOBS_TAG = "knobs", INPUTS_TAG = "inputs", OUTPUT_TAG = "output";

    /**
     * The menu's data: one slot per knob, then the bypass state
     */
    public static final int DATA_BYPASSED = PedalEffectType.MAX_KNOBS, DATA_COUNT = DATA_BYPASSED + 1;

    /**
     * Instruments name the pedal they plug into by this as well as its position,
     * so a new pedal placed where an old one was isn't plugged into by mistake
     */
    private UUID pedalId = UUID.randomUUID();
    /**
     * Whether the saved data had no ID (a pedal from before IDs), so the one it was given is saved
     */
    private boolean idUnsaved = false;
    private PedalEffect effect;
    /**
     * The pedals plugged into this one
     */
    private final Set<BlockPos> inputs = new HashSet<>();
    /**
     * The pedal this one is plugged into, or null
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

    public UUID getPedalId() {
        return pedalId;
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
            PedalMonitorSync.onPedalsChanged(level);
    }

    /**
     * Server only. Makes this a new pedal: unplugged, and with a new ID, even if its item carried a copied pedal's data
     */
    public void resetAsNew() {
        pedalId = UUID.randomUUID();
        inputs.clear();
        output = null;
        setChanged();
    }


    //#region Links

    public Set<BlockPos> getInputs() {
        return Set.copyOf(inputs);
    }
    public void addInput(final BlockPos pedalPos) {
        if (inputs.add(pedalPos.immutable()))
            setChanged();
    }
    public void removeInput(final BlockPos pedalPos) {
        if (inputs.remove(pedalPos))
            setChanged();
    }

    @Nullable
    public BlockPos getOutput() {
        return output;
    }
    public void setOutput(@Nullable final BlockPos target) {
        output = (target == null) ? null : target.immutable();
        setChanged();
    }

    //#endregion


    @Override
    public void onLoad() {
        super.onLoad();
        // Otherwise it would get a new ID every time it loads, and instruments plugged into it would come unplugged
        if (idUnsaved && (level != null) && !level.isClientSide) {
            idUnsaved = false;
            setChanged();
        }
    }

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
        pTag.putUUID(ID_TAG, pedalId);
        pTag.putInt(KNOBS_TAG, effect.knobs());

        if (!inputs.isEmpty()) {
            final ListTag inputList = new ListTag();
            for (final BlockPos input : inputs)
                inputList.add(NbtUtils.writeBlockPos(input));
            pTag.put(INPUTS_TAG, inputList);
        }
        if (output != null)
            pTag.put(OUTPUT_TAG, NbtUtils.writeBlockPos(output));
    }
    @Override
    public void load(final CompoundTag pTag) {
        super.load(pTag);
        idUnsaved = !pTag.hasUUID(ID_TAG);
        if (!idUnsaved)
            pedalId = pTag.getUUID(ID_TAG);
        if (pTag.contains(KNOBS_TAG, Tag.TAG_INT))
            effect = new PedalEffect(typeOf(getBlockState()), pTag.getInt(KNOBS_TAG));

        inputs.clear();
        final ListTag inputList = pTag.getList(INPUTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < inputList.size(); i++)
            inputs.add(NbtUtils.readBlockPos(inputList.getCompound(i)));
        output = pTag.contains(OUTPUT_TAG, Tag.TAG_COMPOUND) ? NbtUtils.readBlockPos(pTag.getCompound(OUTPUT_TAG)) : null;
    }
}
