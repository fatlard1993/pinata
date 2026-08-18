package justfatlard.pinata;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * When a pinata goes up, the children come.
 *
 * <p>A pinata alone is a block you hit. A pinata with every child in the village
 * stood around it is an occasion, and the difference is about twenty lines. It is
 * also the only thing in this suite where the village reacts to something the
 * player did purely because it is fun, which seems worth having.
 *
 * <p>Children only. Adults have jobs, and a farmer abandoning a field to watch
 * somebody hit a sheep-shaped box would read as a bug rather than a party.
 *
 * <p>The walk is driven the way poopsmith drives its villagers: WALK_TARGET
 * written with an expiry, re-asserted on a tick, because villagers are
 * brain-driven and a schedule behaviour will otherwise talk them out of it.
 */
public final class PinataParty {
	private PinataParty() {}

	/** Far enough to pull in a whole village's worth of children. */
	private static final int GATHER_RADIUS = 32;

	/** Slow. They are going to a party, not fleeing one. */
	private static final float WALK_SPEED = 0.5F;
	private static final int CLOSE_ENOUGH = 3;

	/** Re-asserted faster than it expires, so one missed tick does not end the party. */
	private static final int MEMORY_TTL_TICKS = 100;
	private static final int REASSERT_INTERVAL = 40;

	/** Somewhere outside to hang it: clear ground with room to swing. */
	public static BlockPos findPartySpot(ServerLevel world, BlockPos near) {
		for (int attempt = 0; attempt < 32; attempt++) {
			int dx = world.getRandom().nextInt(17) - 8;
			int dz = world.getRandom().nextInt(17) - 8;
			int x = near.getX() + dx;
			int z = near.getZ() + dz;
			int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

			BlockPos spot = new BlockPos(x, y, z);
			if (!world.getBlockState(spot).canBeReplaced()) continue;
			if (!world.getBlockState(spot.below()).isSolidRender()) continue;
			if (!world.getBlockState(spot.above()).isAir()) continue;

			return spot;
		}
		return null;
	}

	/**
	 * Point every child in earshot at the pinata.
	 *
	 * <p>Called on a tick while the block is still standing, so the crowd
	 * reassembles if somebody wanders off and disperses on its own once the
	 * pinata is broken and nothing is re-asserting anything.
	 */
	public static void gather(ServerLevel world, BlockPos pinata) {
		AABB around = new AABB(pinata).inflate(GATHER_RADIUS);
		List<Villager> children = world.getEntities(
			EntityTypeTest.forClass(Villager.class), around, Villager::isBaby);

		for (Villager child : children) {
			// Being frightened outranks being invited.
			if (child.getBrain().isActive(Activity.PANIC)) continue;

			child.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
				new WalkTarget(pinata, WALK_SPEED, CLOSE_ENOUGH), MEMORY_TTL_TICKS);
			child.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET,
				new BlockPosTracker(pinata), MEMORY_TTL_TICKS);
		}
	}

	public static boolean shouldGatherThisTick(long gameTime) {
		return gameTime % REASSERT_INTERVAL == 0;
	}

	/** Pinatas somebody bought a party for, and the crowd that owes them attendance. */
	private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,
		java.util.Set<Long>> PARTIES = new java.util.concurrent.ConcurrentHashMap<>();

	public static void beginParty(ServerLevel world, BlockPos pinata) {
		PARTIES.computeIfAbsent(world.dimension(), key -> java.util.concurrent.ConcurrentHashMap.newKeySet())
			.add(pinata.asLong());
	}

	/**
	 * Re-gather the children at every party still standing.
	 *
	 * <p>A party ends when its pinata does. Nothing announces that, so the block
	 * is checked rather than trusted: broken, exploded, or replaced by somebody
	 * building a house on it all end the same way.
	 */
	public static void tickParties(ServerLevel world) {
		java.util.Set<Long> here = PARTIES.get(world.dimension());
		if (here == null || here.isEmpty()) return;

		here.removeIf(packed -> {
			BlockPos pos = BlockPos.of(packed);
			if (!world.isLoaded(pos)) return false;

			if (!world.getBlockState(pos).is(Pinata.PINATA_BLOCK)) return true;

			gather(world, pos);
			return false;
		});
	}
}
