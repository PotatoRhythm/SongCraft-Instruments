package com.stump.songcraft_instruments.networking.buttonidentifier;

import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class NoteGridButtonIdentifier extends NoteButtonIdentifier {

    public final int column, row;
    @OnlyIn(Dist.CLIENT)
    public NoteGridButtonIdentifier(final NoteGridButton button) {
        this.column = button.column;
        this.row = button.row;
    }

    public NoteGridButtonIdentifier(FriendlyByteBuf buf) {
        column = buf.readInt();
        row = buf.readInt();
    }
    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        super.writeToNetwork(buf);
        buf.writeInt(column);
        buf.writeInt(row);
    }


    @Override
    public boolean matches(NoteButtonIdentifier other) {
        return MatchType.forceMatch(other, this::gridMatch);
    }
    private boolean gridMatch(final NoteGridButtonIdentifier other) {
        return (column == other.column) && (row == other.row);
    }

}