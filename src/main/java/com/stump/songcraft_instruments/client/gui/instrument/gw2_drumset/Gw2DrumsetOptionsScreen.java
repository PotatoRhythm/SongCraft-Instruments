package com.stump.songcraft_instruments.client.gui.instrument.gw2_drumset;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Gw2DrumsetOptionsScreen extends InstrumentOptionsScreen {

    public Gw2DrumsetOptionsScreen(@Nullable InstrumentScreen screen) {
        super(screen);
    }

    @Override
    public INoteLabel[] getLabels() {
        return Gw2DrumsetNoteLabel.availableVals();
    }

    @Override
    public INoteLabel getCurrentLabel() {
        return ModClientConfigs.GW2_DRUMSET_LABEL_TYPE.get();
    }

    @Override
    protected void saveLabel(INoteLabel newLabel) {
        if (newLabel instanceof Gw2DrumsetNoteLabel label) {
            ModClientConfigs.GW2_DRUMSET_LABEL_TYPE.set(label);
        }
    }
}
