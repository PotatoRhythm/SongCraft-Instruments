package com.stump.songcraft_instruments.menu;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.entity.vehicle.LooperVehicleMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SCInstrumentMod.MODID);
    public static void register(final IEventBus bus) {
        MENUS.register(bus);
    }

    public static final RegistryObject<MenuType<LooperVehicleMenu>> LOOPER_VEHICLE = MENUS.register("looper_vehicle", () ->
        new MenuType<>(LooperVehicleMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    // Opened with the pedal's position, see PedalMenu
    public static final RegistryObject<MenuType<PedalMenu>> PEDAL = MENUS.register("pedal", () ->
        IForgeMenuType.create(PedalMenu::new)
    );
}
