package com.culinarycauldron;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Mansion kitchen vindicator: behaves like a vindicator but wields a Ladle (dropped via loot table). */
public class VindicatorChef extends Vindicator {
	public VindicatorChef(EntityType<? extends Vindicator> type, Level level) {
		super(type, level);
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LADLE));
		setDropChance(EquipmentSlot.MAINHAND, 0.0f); // the guaranteed drop comes from the loot table
	}
}
