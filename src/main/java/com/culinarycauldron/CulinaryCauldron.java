package com.culinarycauldron;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

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
		LootTableEvents.MODIFY.register((key, table, source, holder) -> {
			if (key.identifier().equals(Identifier.fromNamespaceAndPath("minecraft", "entities/vindicator"))) {
				table.withPool(LootPool.lootPool()
						.add(LootItem.lootTableItem(ModItems.LADLE))
						.when(Holder.direct(LootItemRandomChanceCondition.randomChance(0.05f).build())));
			}
		});
	}
}
