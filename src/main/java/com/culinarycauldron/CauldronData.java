package com.culinarycauldron;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

/**
 * Cooking state of one vanilla cauldron.
 *
 * @param ingredients  up to 4 stored single-item stacks
 * @param cooked       is_cooked
 * @param pendingSoup  pending_soup_data: the finished soup (hunger, saturation, effects) awaiting a bowl
 * @param soupColor    RGB of the cooked soup (used for the water overlay)
 */
public record CauldronData(List<ItemStack> ingredients, boolean cooked, ItemStack pendingSoup, int soupColor) {
	public static final CauldronData EMPTY = new CauldronData(List.of(), false, ItemStack.EMPTY, 0);

	public static final Codec<CauldronData> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.CODEC.listOf().fieldOf("ingredients").forGetter(CauldronData::ingredients),
			Codec.BOOL.fieldOf("is_cooked").forGetter(CauldronData::cooked),
			ItemStack.OPTIONAL_CODEC.fieldOf("pending_soup_data").forGetter(CauldronData::pendingSoup),
			Codec.INT.optionalFieldOf("soup_color", 0).forGetter(CauldronData::soupColor)
	).apply(i, CauldronData::new));

	public CauldronData withIngredient(ItemStack stack) {
		List<ItemStack> list = new ArrayList<>(ingredients);
		list.add(stack);
		return new CauldronData(List.copyOf(list), false, ItemStack.EMPTY, 0);
	}

	public CauldronData withoutLast() {
		return new CauldronData(List.copyOf(ingredients.subList(0, ingredients.size() - 1)), false, ItemStack.EMPTY, 0);
	}

	public static CauldronData cooked(ItemStack soup, int color) {
		return new CauldronData(List.of(), true, soup, color);
	}
}
