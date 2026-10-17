package com.culinarycauldron.mixin;

import com.culinarycauldron.ModEntities;
import com.culinarycauldron.VindicatorChef;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles the "VindicatorChef" data marker in mansion templates. */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$WoodlandMansionPiece")
public class WoodlandMansionPieceMixin {
	@Inject(method = "handleDataMarker", at = @At("HEAD"), cancellable = true)
	private void culinaryCauldron$spawnChef(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random,
			BoundingBox box, CallbackInfo ci) {
		if (!"VindicatorChef".equals(name)) {
			return;
		}
		VindicatorChef chef = ModEntities.VINDICATOR_CHEF.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (chef != null) {
			chef.setPersistenceRequired();
			chef.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			chef.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
			level.addFreshEntityWithPassengers(chef);
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		ci.cancel();
	}
}
