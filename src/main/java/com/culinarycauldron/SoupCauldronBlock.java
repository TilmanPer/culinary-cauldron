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
		if (soup.data().cooked() && heated && random.nextInt(5) == 0) {
			int color = soup.data().soupColor();
			level.addParticle(new DustParticleOptions(color, 0.7f), pos.getX() + 0.35 + random.nextDouble() * 0.3,
					surface, pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.03, 0);
			if (random.nextBoolean()) {
				level.addParticle(ParticleTypes.BUBBLE, pos.getX() + 0.5, surface, pos.getZ() + 0.5, 0, 0.05, 0);
			}
			if (random.nextInt(4) == 0) {
				level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, surface,
						pos.getZ() + 0.5, 0, 0.02, 0);
			}
		} else if (!soup.data().ingredients().isEmpty() && random.nextInt(8) == 0) {
			level.addParticle(ParticleTypes.BUBBLE, pos.getX() + random.nextDouble() * 0.6 + 0.2,
					surface, pos.getZ() + random.nextDouble() * 0.6 + 0.2, 0, 0.02, 0);
		}
	}
}