package com.culinarycauldron.mixin;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Occasionally swaps a ground-floor 1x2 room for the kitchen (data/minecraft/structure/woodland_mansion/1x2_kitchen.nbt). */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$FirstFloorRoomCollection")
public class WoodlandMansionRoomsMixin {
	/** 1 in N chance that a eligible 1x2 room becomes the kitchen. */
	private static final int KITCHEN_ONE_IN = 8;

	@Inject(method = "get1x2SideEntrance", at = @At("RETURN"), cancellable = true)
	private void culinaryCauldron$kitchen(RandomSource random, boolean isStairs, CallbackInfoReturnable<String> cir) {
		if (!isStairs && random.nextInt(KITCHEN_ONE_IN) == 0) {
			cir.setReturnValue("1x2_kitchen");
		}
	}
}
