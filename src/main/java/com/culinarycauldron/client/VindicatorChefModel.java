package com.culinarycauldron.client;

import com.culinarycauldron.CulinaryCauldron;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

/** Blockbench model "VindicatorChef" ported to the render-state model API. Texture 64x128. */
public class VindicatorChefModel extends EntityModel<IllagerRenderState> implements ArmedModel<IllagerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
			Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, "vindicator_chef"), "main");

	private final ModelPart hat, head, nose, arms, leftArm, rightArm, leftLeg, rightLeg;

	public VindicatorChefModel(ModelPart root) {
		super(root);
		this.hat = root.getChild("hat");
		this.head = root.getChild("head");
		this.nose = root.getChild("nose");
		this.arms = root.getChild("arms");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation none = new CubeDeformation(0.0F);

		// Hat re-pivoted onto the head origin (export had it at y=24) so it turns with the head.
		root.addOrReplaceChild("hat", CubeListBuilder.create()
				.texOffs(16, 109).addBox(-6.0F, -16.0F, -6.0F, 12.0F, 6.0F, 12.0F, none)
				.texOffs(0, 64).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 3.0F, 10.0F, none), PartPose.ZERO);
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, none), PartPose.ZERO);
		root.addOrReplaceChild("nose", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F, none), PartPose.offset(0.0F, -2.0F, 0.0F));
		root.addOrReplaceChild("apron", CubeListBuilder.create()
				.texOffs(2, 78).addBox(-4.0F, -24.0F, -3.0F, 8.0F, 21.0F, 6.0F, new CubeDeformation(0.6F)),
				PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F, none)
				.texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);

		PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create()
				.texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, none)
				.texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, none),
				PartPose.offsetAndRotation(0.0F, 2.95F, -1.05F, -0.7505F, 0.0F, 0.0F));
		arms.addOrReplaceChild("mirrored", CubeListBuilder.create()
				.texOffs(44, 22).mirror().addBox(4.0F, -23.05F, -3.05F, 4.0F, 8.0F, 4.0F, none).mirror(false),
				PartPose.offset(0.0F, 21.05F, 1.05F));

		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none).mirror(false),
				PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none).mirror(false),
				PartPose.offset(2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-2.0F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 128);
	}

	@Override
	public void setupAnim(IllagerRenderState state) {
		super.setupAnim(state);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		hat.xRot = head.xRot;
		hat.yRot = head.yRot;
		hat.zRot = head.zRot;
		nose.xRot = head.xRot;
		nose.yRot = head.yRot;

		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed * 0.5F;
		leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed * 0.5F;

		arms.visible = false; // crossed-arms part stays unused: the ladle hand needs the free right arm
		if (state.armPose == AbstractIllager.IllagerArmPose.ATTACKING) {
			rightArm.xRot = -1.8F - Mth.sin(state.ageInTicks * 0.25F) * 1.0F;
			leftArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
		} else {
			rightArm.xRot = -0.9F + Mth.sin(state.ageInTicks * 0.067F) * 0.05F; // ladle held out front
			leftArm.xRot = Mth.cos(pos * 0.6662F) * 2.0F * speed * 0.5F;
		}
	}

	@Override
	public void translateToHand(IllagerRenderState state, HumanoidArm arm, PoseStack poseStack) {
		this.root.translateAndRotate(poseStack);
		(arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(poseStack);
	}
}
