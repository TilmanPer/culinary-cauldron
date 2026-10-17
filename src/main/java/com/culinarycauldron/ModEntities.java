package com.culinarycauldron;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.illager.Vindicator;

public final class ModEntities {
	public static final EntityType<VindicatorChef> VINDICATOR_CHEF = register();

	private static EntityType<VindicatorChef> register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
				Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, "vindicator_chef"));
		EntityType<VindicatorChef> type = EntityType.Builder.of(VindicatorChef::new, MobCategory.MONSTER)
				.sized(0.6f, 1.95f).eyeHeight(1.6875f).ridingOffset(-0.6f).clientTrackingRange(8).build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(VINDICATOR_CHEF, Vindicator.createAttributes());
	}

	private ModEntities() {
	}
}
