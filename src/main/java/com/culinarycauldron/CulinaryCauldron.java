package com.culinarycauldron;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class CulinaryCauldron implements ModInitializer {
	public static final String MOD_ID = "custom_mod";

	@Override
	public void onInitialize() {
		ModItems.init();
		ModBlocks.init();
		CauldronCooking.init();
		ResourceKey<CreativeModeTab> toolsAndUtilities = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
				Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"));
		CreativeModeTabEvents.modifyOutputEvent(toolsAndUtilities)
				.register(entries -> entries.accept(ModItems.LADLE));
	}
}
