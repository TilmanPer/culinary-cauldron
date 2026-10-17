package com.culinarycauldron;

import com.culinarycauldron.client.VindicatorChefModel;
import com.culinarycauldron.client.VindicatorChefRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CulinaryCauldronClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockColorRegistry.register((state, level, pos, tintValues) -> {
			Object renderData = ((FabricBlockGetter) level).getBlockEntityRenderData(pos);
			int rgb = renderData instanceof Integer value ? value : 0x3F76E4;
			tintValues.add(0xFF000000 | rgb);
		}, ModBlocks.SOUP_CAULDRON);

		ModelLayerRegistry.registerModelLayer(VindicatorChefModel.LAYER, VindicatorChefModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.VINDICATOR_CHEF, VindicatorChefRenderer::new);
	}
}
