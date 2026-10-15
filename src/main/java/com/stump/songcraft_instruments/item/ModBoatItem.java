package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperOwners;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Places one of this mod's boats, the same way a vanilla boat item does
 */
public class ModBoatItem extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    @FunctionalInterface
    public interface BoatFactory {
        Boat create(Level level, double x, double y, double z);
    }

    private final Boat.Type type;
    private final BoatFactory factory;

    public ModBoatItem(final Boat.Type type, final BoatFactory factory, final Properties properties) {
        super(properties);
        this.type = type;
        this.factory = factory;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);

        // Don't place the boat inside an entity the player is looking through
        final Vec3 viewVector = player.getViewVector(1.0F);
        final List<Entity> entities = level.getEntities(player,
            player.getBoundingBox().expandTowards(viewVector.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
        final Vec3 eyePos = player.getEyePosition();
        for (final Entity entity : entities) {
            final AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eyePos))
                return InteractionResultHolder.pass(stack);
        }

        final Vec3 location = hitResult.getLocation();
        final Boat boat = factory.create(level, location.x, location.y, location.z);
        boat.setVariant(type);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox()))
            return InteractionResultHolder.fail(stack);

        if (!level.isClientSide) {
            final boolean isLooper = LooperOwners.isLooperVehicle(boat);
            if (isLooper && LooperOwners.refusePlacement(player))
                return InteractionResultHolder.fail(stack);

            if (stack.hasCustomHoverName())
                boat.setCustomName(stack.getHoverName());
            level.addFreshEntity(boat);
            if (isLooper)
                LooperOwners.get(level.getServer()).addVehicle(boat.getUUID(), player.getUUID());
            level.gameEvent(player, GameEvent.ENTITY_PLACE, location);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
