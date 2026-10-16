package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.block.SpeakerBlock;
import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.item.clientExtensions.InstrumentItemClientExt;
import com.stump.songcraft_instruments.networking.OpenInstrumentPacketSender;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import com.stump.songcraft_instruments.pedal.InstrumentPedals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * An item responsible for opening an {@link InstrumentScreen}.
 */
public class InstrumentItem extends Item {

    protected final OpenInstrumentPacketSender onOpenRequest;
    /**
     * @param onOpenRequest A server-side event fired when the player has requested to interact
     * with the instrument.
     * It should send a packet to the given player for opening this instrument's screen.
     */
    public InstrumentItem(final OpenInstrumentPacketSender onOpenRequest) {
        this(onOpenRequest, new Properties());
    }
    /**
     * @param onOpenRequest A server-side event fired when the player has requested to interact
     * with the instrument.
     * It should send a packet to the given player for opening this instrument's screen.
     * @param properties The properties of this instrument item. {@link Properties#stacksTo stack size}
     * will always be set to 1.
     */
    public InstrumentItem(final OpenInstrumentPacketSender onOpenRequest, final Properties properties) {
        super(properties.stacksTo(1));
        this.onOpenRequest = onOpenRequest;
    }
    

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        final ItemStack item = pPlayer.getItemInHand(pUsedHand);

        if (pLevel.isClientSide)
            return InteractionResultHolder.success(item);

        if (InstrumentPacketUtil.sendOpenPacket((ServerPlayer)pPlayer, pUsedHand, onOpenRequest)) {
            return InteractionResultHolder.success(item);
        } else {
            return InteractionResultHolder.fail(item);
        }
    }

    /**
     * right-clicking a speaker pairs this instrument to it, and shift + right-clicking unpairs it.
     * right-clicking an effects pedal plugs this instrument into it, and shift + right-clicking unplugs it.
     * handled here rather than in the block's own interaction, as this runs first and regardless of sneaking.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        final Level level = context.getLevel();
        final Block block = level.getBlockState(context.getClickedPos()).getBlock();
        if (block instanceof SpeakerBlock)
            return SpeakerBlock.onHeldInstrumentUse(level, context.getClickedPos(), player, stack);

        if (block instanceof PedalBlock) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            if (level.getBlockEntity(context.getClickedPos()) instanceof PedalBlockEntity pedal)
                InstrumentPedals.useHeldInstrument(player, stack, pedal);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.CUSTOM;
    }
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new InstrumentItemClientExt());
    }
    
}
