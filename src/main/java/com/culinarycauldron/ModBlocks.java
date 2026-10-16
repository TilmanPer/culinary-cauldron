package com.culinarycauldron;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlocks {
	public static final Block SOUP_CAULDRON = registerSoupCauldron();

	public static final BlockEntityType<SoupCauldronBlockEntity> SOUP_CAULDRON_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
					Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, "soup_cauldron")),
			new BlockEntityType<>(SoupCauldronBlockEntity::new, Set.of(SOUP_CAULDRON)));

	private static Block registerSoupCauldron() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, "soup_cauldron"));
		return Registry.register(BuiltInRegistries.BLOCK, key,
				new SoupCauldronBlock(Block.Properties.ofFullCopy(Blocks.WATER_CAULDRON).setId(key)));
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}