package com.culinarycauldron.mixin.client;

import com.culinarycauldron.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public class CampfireSmokeMixin {
	@Inject(method = "makeParticles", at = @At("HEAD"), cancellable = true)
	private static void customMod$hideCampfireSmoke(Level level, BlockPos pos, boolean signal, boolean lit,
			CallbackInfo callbackInfo) {
		BlockState above = level.getBlockState(pos.above());
		if (above.is(Blocks.WATER_CAULDRON) || above.is(ModBlocks.SOUP_CAULDRON)) {
			callbackInfo.cancel();
		}
	}

	@Inject(method = "animateTick", at = @At("TAIL"))
	private void customMod$animateWaterCauldron(BlockState state, Level level, BlockPos pos, RandomSource random,
			CallbackInfo callbackInfo) {
		BlockState above = level.getBlockState(pos.above());
		if (!above.is(Blocks.WATER_CAULDRON) || !above.hasProperty(LayeredCauldronBlock.LEVEL)
				|| random.nextInt(4) != 0) {
			return;
		}
		double surface = pos.getY() + 1.0 + (6 + above.getValue(LayeredCauldronBlock.LEVEL) * 3) / 16.0;
		int bubbles = 1 + random.nextInt(2);
		for (int i = 0; i < bubbles; i++) {
			double bubbleX = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double bubbleZ = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			level.addParticle(ParticleTypes.BUBBLE, bubbleX, surface, bubbleZ, 0, 0.04, 0);
			if (random.nextBoolean()) {
				level.addParticle(ParticleTypes.BUBBLE_POP, bubbleX, surface, bubbleZ, 0, 0, 0);
			}
		}
	}
}