package com.culinarycauldron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class CauldronCooking {
	public static final int MAX_INGREDIENTS = 3;

	/** Persistent per-dimension storage: cauldron position (BlockPos#asLong as string) -> state. */
	private static final AttachmentType<Map<String, CauldronData>> STATES =
			AttachmentRegistry.<Map<String, CauldronData>>create(
					Identifier.fromNamespaceAndPath(CulinaryCauldron.MOD_ID, "cauldron_states"),
					builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, CauldronData.CODEC)));

	public static void init() {
		UseBlockCallback.EVENT.register(CauldronCooking::onUse);

		// Give ingredients back if the cauldron is broken mid-cook.
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server && state.is(Blocks.WATER_CAULDRON)) {
				Map<String, CauldronData> map = new HashMap<>(server.getAttachedOrElse(STATES, Map.of()));
				CauldronData data = map.remove(key(pos));
				if (data != null) {
					server.setAttached(STATES, map);
					clearDisplay(server, pos);
					clearWaterOverlay(server, pos);
					data.ingredients().forEach(s -> Block.popResource(server, pos, s.copy()));
				}
			}
		});
	}

	// ---------------------------------------------------------------- interaction

	private static InteractionResult onUse(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator() || player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		if (!isCookingSetup(level, pos)) {
			return InteractionResult.PASS;
		}

		ItemStack held = player.getItemInHand(hand);
		boolean ladle = held.is(ModItems.LADLE);
		boolean bowl = held.is(Items.BOWL);
		boolean potion = held.is(Items.POTION);
		boolean ingredient = isValidIngredient(held);
		boolean emptyHand = held.isEmpty() && hand == InteractionHand.MAIN_HAND;
		if (!ladle && !bowl && !ingredient && !emptyHand) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS; // client-side prediction (swing); server is authoritative
		}

		Map<String, CauldronData> map = new HashMap<>(server.getAttachedOrElse(STATES, Map.of()));
		String key = key(pos);
		CauldronData data = map.getOrDefault(key, CauldronData.EMPTY);

		if (ladle) {
			if (data.cooked()) {
				return InteractionResult.PASS;
			}
			if (data.ingredients().size() != MAX_INGREDIENTS) {
				player.sendOverlayMessage(Component.translatable("message.custom_mod.need_three"));
				return InteractionResult.SUCCESS;
			}
			for (ItemStack used : data.ingredients()) {
				if (used.is(Items.POTION)) {
					give(player, new ItemStack(Items.GLASS_BOTTLE)); // empty bottle comes back once stirred in
				}
			}
			int soupColor = averageColor(data.ingredients());
			map.put(key, CauldronData.cooked(buildSoup(data.ingredients()), soupColor));
			server.setAttached(STATES, map);
			clearDisplay(server, pos);
			setWaterOverlay(server, pos, soupColor);
			play(server, pos, SoundEvents.BREWING_STAND_BREW);
			server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					12, 0.2, 0.1, 0.2, 0.02);
			server.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
					10, 0.2, 0.05, 0.2, 0.0);
			return InteractionResult.SUCCESS;
		}

		if (bowl) {
			if (!data.cooked() || data.pendingSoup().isEmpty()) {
				return InteractionResult.PASS;
			}
			ItemStack soup = data.pendingSoup().copy();
			held.consume(1, player);
			give(player, soup);
			// One soup per water level (3 from a full cauldron); the cauldron stays "cooked" until empty.
			BlockState state = level.getBlockState(pos);
			LayeredCauldronBlock.lowerFillLevel(state, level, pos);
			if (level.getBlockState(pos).is(Blocks.WATER_CAULDRON)) {
				setWaterOverlay(server, pos, data.soupColor());
			} else {
				map.remove(key);
				server.setAttached(STATES, map);
				clearWaterOverlay(server, pos);
			}
			play(server, pos, SoundEvents.BOTTLE_FILL);
			return InteractionResult.SUCCESS;
		}

		if (emptyHand) {
			// Retrieve the most recently added ingredient
			if (data.cooked() || data.ingredients().isEmpty()) {
				return InteractionResult.PASS;
			}
			List<ItemStack> list = data.ingredients();
			ItemStack back = list.get(list.size() - 1).copy();
			CauldronData updated = data.withoutLast();
			map.put(key, updated);
			server.setAttached(STATES, map);
			give(player, back);
			refreshDisplay(server, pos, updated.ingredients());
			play(server, pos, SoundEvents.GENERIC_SPLASH);
			return InteractionResult.SUCCESS;
		}

		// Adding an ingredient
		if (data.cooked() || data.ingredients().size() >= MAX_INGREDIENTS) {
			return InteractionResult.PASS;
		}
		if (potion && data.ingredients().stream().anyMatch(s -> s.is(Items.POTION))) {
			return InteractionResult.PASS; // max 1 potion
		}
		ItemStack single = held.copyWithCount(1);
		held.consume(1, player);
		CauldronData updated = data.withIngredient(single);
		map.put(key, updated);
		server.setAttached(STATES, map);
		refreshDisplay(server, pos, updated.ingredients());
		play(server, pos, SoundEvents.GENERIC_SPLASH);
		server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
				8, 0.2, 0.05, 0.2, 0.0);
		return InteractionResult.SUCCESS;
	}

	// ---------------------------------------------------------------- rules

	/** Water cauldron directly on top of a lit campfire / soul campfire. */
	private static boolean isCookingSetup(Level level, BlockPos pos) {
		if (!level.getBlockState(pos).is(Blocks.WATER_CAULDRON)) {
			return false;
		}
		BlockState below = level.getBlockState(pos.below());
		return (below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) && below.getValue(CampfireBlock.LIT);
	}

	private static boolean isValidIngredient(ItemStack stack) {
		if (stack.isEmpty() || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(ModItems.CUSTOM_SOUP)) {
			return false;
		}
		return stack.is(Items.POTION) || stack.has(DataComponents.FOOD);
	}

	// ---------------------------------------------------------------- soup building

	private static ItemStack buildSoup(List<ItemStack> ingredients) {
		int hunger = 0;
		float saturation = 0.0f;
		List<MobEffectInstance> effects = new ArrayList<>();

		for (ItemStack stack : ingredients) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null) {
				hunger += food.nutrition();
				saturation += food.saturation();
			}
			// Potion effects
			PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
			if (potion != null) {
				for (MobEffectInstance effect : potion.getAllEffects()) {
					effects.add(effect);
				}
			}
			// Food effects (golden apple, rotten flesh, poisonous potato, pufferfish, spider eye...)
			Consumable consumable = stack.get(DataComponents.CONSUMABLE);
			if (consumable != null) {
				for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
					if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect apply) {
						effects.addAll(apply.effects());
					}
				}
			}
		}

		hunger /= 2;
		saturation /= 2.0f;
		ItemStack soup = new ItemStack(ModItems.CUSTOM_SOUP);
		soup.set(DataComponents.FOOD, new FoodProperties(hunger, saturation, true));
		List<MobEffectInstance> merged = merge(effects);
		soup.set(DataComponents.CUSTOM_NAME, Component.literal(soupName(hunger, saturation, merged)));
		if (!merged.isEmpty()) { // avoid a "No Effects" tooltip line
			PotionContents contents = PotionContents.EMPTY;
			for (MobEffectInstance effect : merged) {
				contents = contents.withEffectAdded(effect);
			}
			soup.set(DataComponents.POTION_CONTENTS, contents);
		}
		return soup;
	}

	private static String soupName(int hunger, float saturation, List<MobEffectInstance> effects) {
		Map<String, Double> scores = new LinkedHashMap<>();
		if (hunger > 0) {
			double foodScore = foodScore(hunger, saturation);
			if (saturation / hunger > 1.2f) {
				scores.put("Sustaining", foodScore);
			} else {
				scores.put("Hearty", foodScore);
			}
		}
		for (MobEffectInstance effect : effects) {
			String category = effectCategory(effect);
			if (category != null) {
				double effectScore = (effect.getAmplifier() + 1.0) * effect.getDuration() / 20.0 * 3.0;
				scores.merge(category, effectScore, Double::sum);
			}
		}

		String category = "Hearty";
		double score = hunger;
		for (Map.Entry<String, Double> entry : scores.entrySet()) {
			if (entry.getValue() > score) {
				category = entry.getKey();
				score = entry.getValue();
			}
		}

		String grade = score < 10.0 ? "Thin " : score >= 25.0 ? "Potent " : score >= 15.0 ? "Rich " : "";
		return grade + category + " Stew";
	}

	private static double foodScore(int hunger, float saturation) {
		return hunger + saturation;
	}

	private static String effectCategory(MobEffectInstance effect) {
		String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).getPath();
		return switch (id) {
			case "regeneration", "instant_health", "saturation", "absorption", "health_boost" -> "Mending";
			case "speed", "haste", "strength", "jump_boost", "resistance", "slow_falling", "dolphins_grace" -> "Brawny";
			case "night_vision", "invisibility", "glowing", "conduit_power" -> "Luminous";
			case "luck", "hero_of_the_village" -> "Blessed";
			case "slowness", "mining_fatigue", "weakness", "blindness", "darkness", "nausea", "hunger" -> "Enfeebling";
			case "poison", "fatal_poison", "wither", "instant_damage" -> "Toxic";
			case "levitation", "bad_omen", "unluck" -> "Ethereal";
			default -> null;
		};
	}

	/** One instance per effect type: highest amplifier wins, then longest duration. */
	private static List<MobEffectInstance> merge(List<MobEffectInstance> effects) {
		Map<Holder<MobEffect>, MobEffectInstance> best = new LinkedHashMap<>();
		for (MobEffectInstance e : effects) {
			MobEffectInstance old = best.get(e.getEffect());
			if (old == null || e.getAmplifier() > old.getAmplifier()
					|| (e.getAmplifier() == old.getAmplifier() && e.getDuration() > old.getDuration())) {
				best.put(e.getEffect(), new MobEffectInstance(e));
			}
		}
		return new ArrayList<>(best.values());
	}


	// ---------------------------------------------------------------- visual ingredients

	private static String displayTag(BlockPos pos) {
		return "cc_" + Long.toUnsignedString(pos.asLong());
	}

	private static void runCommand(ServerLevel level, String command) {
		var server = level.getServer();
		var source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, command);
	}

	private static void clearDisplay(ServerLevel level, BlockPos pos) {
		runCommand(level, "kill @e[type=minecraft:item_display,tag=" + displayTag(pos) + "]");
	}


	// ---------------------------------------------------------------- water colour

	private static final int DEFAULT_SOUP = 0x835432; // brown
	private static final int WATER_BLUE = 0x3F76E4;

	/** Dye colours -> matching stained glass block (vanilla cauldron water can't be tinted server-side). */
	private static final String[] GLASS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
	private static final int[] GLASS_RGB = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
			0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

	private static final Map<String, Integer> FOOD_COLORS = Map.ofEntries(
			Map.entry("apple", 0xC8312B), Map.entry("golden_apple", 0xF5C842), Map.entry("golden_carrot", 0xE8B030),
			Map.entry("carrot", 0xED8A1C), Map.entry("potato", 0xC9A15B), Map.entry("baked_potato", 0xB8863B),
			Map.entry("poisonous_potato", 0x9DB04A), Map.entry("beetroot", 0x8B2A3A), Map.entry("melon_slice", 0xD8473C),
			Map.entry("sweet_berries", 0xB0313A), Map.entry("glow_berries", 0xE9A63A), Map.entry("chorus_fruit", 0x8E6AA0),
			Map.entry("dried_kelp", 0x3F5B35), Map.entry("bread", 0xC8934E), Map.entry("cookie", 0xB8793A),
			Map.entry("pumpkin_pie", 0xD07A2E), Map.entry("beef", 0xB5483F), Map.entry("cooked_beef", 0x7A4A2F),
			Map.entry("porkchop", 0xE59A9A), Map.entry("cooked_porkchop", 0xB07B4F), Map.entry("mutton", 0xB2514A),
			Map.entry("cooked_mutton", 0x7C4B35), Map.entry("chicken", 0xE6B8A0), Map.entry("cooked_chicken", 0xB98242),
			Map.entry("rabbit", 0xD9A27E), Map.entry("cooked_rabbit", 0x9A6A3E), Map.entry("cod", 0xC9A98A),
			Map.entry("cooked_cod", 0xB88D5C), Map.entry("salmon", 0xD6644E), Map.entry("cooked_salmon", 0xC9754B),
			Map.entry("tropical_fish", 0xF08A2B), Map.entry("pufferfish", 0xD9C94B), Map.entry("rotten_flesh", 0x6E7F4A),
			Map.entry("spider_eye", 0x7A2B3A), Map.entry("honey_bottle", 0xE8A317), Map.entry("mushroom_stew", 0xA9795A),
			Map.entry("rabbit_stew", 0xA06B3F), Map.entry("beetroot_soup", 0x7A1F33), Map.entry("suspicious_stew", 0xA58A4B));

	private static int colorOf(ItemStack stack) {
		if (stack.is(Items.POTION)) {
			PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
			int r = 0, g = 0, b = 0, n = 0;
			if (potion != null) {
				for (MobEffectInstance e : potion.getAllEffects()) {
					int c = e.getEffect().value().getColor();
					r += (c >> 16) & 255;
					g += (c >> 8) & 255;
					b += c & 255;
					n++;
				}
			}
			return n == 0 ? WATER_BLUE : ((r / n) << 16) | ((g / n) << 8) | (b / n);
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return FOOD_COLORS.getOrDefault(path, DEFAULT_SOUP);
	}

	private static int averageColor(List<ItemStack> ingredients) {
		if (ingredients.isEmpty()) {
			return DEFAULT_SOUP;
		}
		int r = 0, g = 0, b = 0;
		for (ItemStack stack : ingredients) {
			int c = colorOf(stack);
			r += (c >> 16) & 255;
			g += (c >> 8) & 255;
			b += c & 255;
		}
		int n = ingredients.size();
		return ((r / n) << 16) | ((g / n) << 8) | (b / n);
	}

	private static String nearestGlass(int rgb) {
		int best = 12; // brown
		long bestDist = Long.MAX_VALUE;
		for (int i = 0; i < GLASS.length; i++) {
			long dr = ((rgb >> 16) & 255) - ((GLASS_RGB[i] >> 16) & 255);
			long dg = ((rgb >> 8) & 255) - ((GLASS_RGB[i] >> 8) & 255);
			long db = (rgb & 255) - (GLASS_RGB[i] & 255);
			long d = dr * dr + dg * dg + db * db;
			if (d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return GLASS[best] + "_stained_glass";
	}

	private static String waterTag(BlockPos pos) {
		return "ccw_" + Long.toUnsignedString(pos.asLong());
	}

	private static double surfaceY(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		int water = state.hasProperty(LayeredCauldronBlock.LEVEL) ? state.getValue(LayeredCauldronBlock.LEVEL) : 1;
		return pos.getY() + (6 + water * 3) / 16.0;
	}

	private static void clearWaterOverlay(ServerLevel level, BlockPos pos) {
		runCommand(level, "kill @e[type=minecraft:block_display,tag=" + waterTag(pos) + "]");
	}

	/** A thin tinted glass pane lying just above the water surface. */
	private static void setWaterOverlay(ServerLevel level, BlockPos pos, int rgb) {
		clearWaterOverlay(level, pos);
		runCommand(level, String.format(java.util.Locale.ROOT,
				"summon minecraft:block_display %.3f %.3f %.3f {block_state:{Name:\"minecraft:%s\"},"
						+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
						+ "scale:[0.75f,0.01f,0.75f],translation:[0f,0f,0f]},Tags:[\"%s\"]}",
				pos.getX() + 0.125, surfaceY(level, pos) + 0.004, pos.getZ() + 0.125,
				nearestGlass(rgb), waterTag(pos)));
	}

	/** Floats each stored ingredient on the water surface, tilted and partly submerged (4 slots). */
	private static void refreshDisplay(ServerLevel level, BlockPos pos, List<ItemStack> ingredients) {
		clearDisplay(level, pos);
		if (ingredients.isEmpty()) {
			clearWaterOverlay(level, pos);
		} else {
			setWaterOverlay(level, pos, averageColor(ingredients));
		}
		double y = surfaceY(level, pos) - 0.02;
		double[][] slots = {{-0.17, -0.17}, {0.17, -0.15}, {-0.15, 0.18}, {0.18, 0.16}};
		double[] yaw = {20, 115, 200, 290};        // spin around vertical axis (degrees)
		double[] tiltX = {28, -22, 18, -32};       // pitch away from flat
		double[] tiltZ = {-15, 24, 30, -20};       // roll away from flat
		double[] bob = {0.00, 0.03, -0.01, 0.02};  // slight height variation
		for (int i = 0; i < ingredients.size() && i < slots.length; i++) {
			String id = BuiltInRegistries.ITEM.getKey(ingredients.get(i).getItem()).toString();
			// q = Yaw * TiltZ * (lie flat + TiltX); the right-most rotation is applied to the model first
			double[] q = quatMul(axis(0, 1, 0, yaw[i]),
					quatMul(axis(0, 0, 1, tiltZ[i]), axis(1, 0, 0, 90 + tiltX[i])));
			runCommand(level, String.format(java.util.Locale.ROOT,
					"summon minecraft:item_display %.3f %.3f %.3f {item:{id:\"%s\",count:1},"
							+ "transformation:{left_rotation:[%.4ff,%.4ff,%.4ff,%.4ff],right_rotation:[0f,0f,0f,1f],"
							+ "scale:[0.32f,0.32f,0.32f],translation:[0f,0f,0f]},Tags:[\"%s\"]}",
					pos.getX() + 0.5 + slots[i][0], y + bob[i], pos.getZ() + 0.5 + slots[i][1], id,
					q[0], q[1], q[2], q[3], displayTag(pos)));
		}
	}

	/** Quaternion {x, y, z, w} for a rotation of {@code degrees} around a unit axis. */
	private static double[] axis(double x, double y, double z, double degrees) {
		double h = Math.toRadians(degrees) / 2.0;
		double s = Math.sin(h);
		return new double[] {x * s, y * s, z * s, Math.cos(h)};
	}

	/** Hamilton product a * b. */
	private static double[] quatMul(double[] a, double[] b) {
		return new double[] {
				a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1],
				a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
				a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3],
				a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2]};
	}

	// ---------------------------------------------------------------- helpers

	private static String key(BlockPos pos) {
		return Long.toString(pos.asLong());
	}

	private static void give(Player player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
	}

	private static void play(ServerLevel level, BlockPos pos, SoundEvent sound) {
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
	}

	private CauldronCooking() {
	}
}
