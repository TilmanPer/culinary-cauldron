package com.culinarycauldron;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SoupCauldronBlockEntity extends BlockEntity {
	private CauldronData data = CauldronData.EMPTY;

	public SoupCauldronBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.SOUP_CAULDRON_ENTITY, pos, state);
	}

	public CauldronData data() {
		return data;
	}

	public void setData(CauldronData data) {
		this.data = data;
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			if (state.getValue(SoupCauldronBlock.COOKED) != data.cooked()) {
				level.setBlock(worldPosition, state.setValue(SoupCauldronBlock.COOKED, data.cooked()), Block.UPDATE_ALL);
				state = getBlockState();
			}
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
			level.setBlocksDirty(worldPosition, state, state);
			if (level instanceof ServerLevel server) {
				Packet<ClientGamePacketListener> packet = getUpdatePacket();
				for (ServerPlayer player : server.getChunkSource().chunkMap
						.getPlayers(new ChunkPos(worldPosition.getX() >> 4, worldPosition.getZ() >> 4), false)) {
					player.connection.send(packet);
				}
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		data = input.read("data", CauldronData.CODEC).orElseGet(() -> new CauldronData(List.of(),
				input.getBooleanOr("is_cooked", false), ItemStack.EMPTY, input.getIntOr("soup_color", 0)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.store("data", CauldronData.CODEC, data);
	}

	@Override
	public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("soup_color", data.soupColor());
		tag.putBoolean("is_cooked", data.cooked());
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public Object getRenderData() {
		return data.cooked() ? data.soupColor() : 0x3F76E4;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide() && !data.ingredients().isEmpty()) {
			for (ItemStack ingredient : data.ingredients()) {
				Block.popResource(level, pos, ingredient.copy());
			}
		}
		super.preRemoveSideEffects(pos, state);
	}
}