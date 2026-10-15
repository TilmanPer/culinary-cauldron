package com.culinarycauldron;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class CulinaryCauldron implements ModInitializer {
	public static final String MOD_ID = "custom_mod";

	@Override
	public void onInitialize() {
		ModItems.init();
		CauldronCooking.init();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS)
				.register(entries -> entries.accept(ModItems.LADLE));
	}
}
