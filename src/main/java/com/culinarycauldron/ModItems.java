package com.culinarycauldron;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ModItems {
	/** Unbreakable tool-like item, stack size 1. */
	public static final Item LADLE = register("ladle", Item::new,
			new Item.Properties().stacksTo(1));

	/** Soup: stack size 16. Hunger/saturation (FOOD) and effects (POTION_CONTENTS) are set per stack. */
	public static final Item CUSTOM_SOUP = register("custom_soup", SoupItem::new,
			new Item.Properties().stacksTo(16)
					.food(new FoodProperties(0, 0.0f, true))
					.usingConvertsTo(Items.BOWL));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	public static void init() {
	}

	private ModItems() {
	}
}
