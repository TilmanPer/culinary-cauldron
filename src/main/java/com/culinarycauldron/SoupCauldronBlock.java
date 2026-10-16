package com.culinarycauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;

public class SoupCauldronBlock extends LayeredCauldronBlock implements EntityBlock {
	public static final BooleanProperty COOKED = BooleanProperty.create("cooked");

	public SoupCauldronBlock(Properties properties) {
		super(Biome.Precipitation.NONE, new CauldronInteraction.Dispatcher(), properties);
		registerDefaultState(defaultBlockState().setValue(COOKED, false));
	}

	@Override
	protected void createBlockStateDefinition(
			net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(COOKED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SoupCauldronBlockEntity(pos, state);
	}

	@Override
	protected ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos,
			BlockState state, boolean includeData) {
		return new ItemStack(Blocks.CAULDRON);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof SoupCauldronBlockEntity soup)) {
			return;
		}
		BlockState below = level.getBlockState(pos.below());
		boolean heated = (below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE))
				&& below.getValue(CampfireBlock.LIT);
		double surface = pos.getY() + (6 + state.getValue(LEVEL) * 3) / 16.0;
		if (heated && (soup.data().cooked() || !soup.data().ingredients().isEmpty()) && random.nextInt(4) == 0) {
			if (soup.data().cooked()) {
				int color = soup.data().soupColor();
				level.addParticle(new DustParticleOptions(color, 0.7f), pos.getX() + 0.25 + random.nextDouble() * 0.5,
						surface, pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0.03, 0);
			}
			int bubbles = 1 + random.nextInt(2);
			for (int i = 0; i < bubbles; i++) {
				double bubbleX = pos.getX() + 0.2 + random.nextDouble() * 0.6;
				double bubbleZ = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
				level.addParticle(ParticleTypes.BUBBLE, bubbleX, surface, bubbleZ, 0, 0.04, 0);
				if (random.nextBoolean()) {
					level.addParticle(ParticleTypes.BUBBLE_POP, bubbleX, surface, bubbleZ, 0, 0, 0);
				}
			}
			if (soup.data().cooked() && random.nextInt(4) == 0) {
				level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, surface,
						pos.getZ() + 0.5, 0, 0.02, 0);
			}
		}
	}
}