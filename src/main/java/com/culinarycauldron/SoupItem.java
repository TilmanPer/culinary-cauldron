package com.culinarycauldron;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

/**
 * Hunger/saturation come from the stack's FOOD component, the tooltip comes from its
 * POTION_CONTENTS component (vanilla potion-style, positive and negative effects),
 * and the effects stored there are applied once the soup is eaten.
 */
public class SoupItem extends Item {
	public SoupItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (level instanceof ServerLevel && contents != null) {
			for (MobEffectInstance effect : contents.customEffects()) {
				entity.addEffect(new MobEffectInstance(effect));
			}
		}
		return result;
	}
}
