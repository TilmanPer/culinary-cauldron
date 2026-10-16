package com.culinarycauldron;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;

public class CulinaryCauldronClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockColorRegistry.register((state, level, pos, tintValues) -> {
			Object renderData = ((FabricBlockGetter) level).getBlockEntityRenderData(pos);
			int rgb = renderData instanceof Integer value ? value : 0x3F76E4;
			tintValues.add(0xFF000000 | rgb);
		}, ModBlocks.SOUP_CAULDRON);
	}
}