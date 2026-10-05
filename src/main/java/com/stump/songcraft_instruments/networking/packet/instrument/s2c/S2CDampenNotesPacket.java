package com.stump.songcraft_instruments.networking.packet.instrument.s2c;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.sound.NoteSoundInstances;
import com.stump.songcraft_instruments.sound.held.HeldNoteSounds;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

public class S2CDampenNotesPacket implements IModPacket {

    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private final int initiatorId;

    public S2CDampenNotesPacket(int initiatorId) {
        this.initiatorId = initiatorId;
    }

    public S2CDampenNotesPacket(FriendlyByteBuf buf) {
        this.initiatorId = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(initiatorId);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(Context context) {
        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null)
            return;

        final var entity = minecraft.level.getEntity(initiatorId);

        if (entity == null)
            return;

        // Without server audio, the player's own notes play locally and were already dampened when they sent this.
        // Dampening again here would cut off any note they started since, like a single-note instrument's next note.
        if ((entity == minecraft.player) && !ModClientConfigs.SERVER_AUDIO.get())
            return;

        final InitiatorID source = InitiatorID.fromEntity(entity);

        NoteSoundInstances.dampenAll(initiatorId);
        NoteSoundInstances.dampenAll(source);
        HeldNoteSounds.dampenAll(source);
    }
}