package com.culinarycauldron.client;

import com.culinarycauldron.CulinaryCauldron;
import com.culinarycauldron.VindicatorChef;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

public class VindicatorChefRenderer extends MobRenderer<VindicatorChef, IllagerRenderState, VindicatorChefModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID,
			"textures/entity/vindicator_chef.png");

	public VindicatorChefRenderer(EntityRendererProvider.Context context) {
		super(context, new VindicatorChefModel(context.bakeLayer(VindicatorChefModel.LAYER)), 0.5F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public IllagerRenderState createRenderState() {
		return new IllagerRenderState();
	}

	@Override
	public Identifier getTextureLocation(IllagerRenderState state) {
		return TEXTURE;
	}

	@Override
	public void extractRenderState(VindicatorChef entity, IllagerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTick);
		state.armPose = entity.isAggressive() ? AbstractIllager.IllagerArmPose.ATTACKING
				: AbstractIllager.IllagerArmPose.NEUTRAL;
	}
}
