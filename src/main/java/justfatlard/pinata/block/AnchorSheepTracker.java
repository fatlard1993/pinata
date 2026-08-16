package justfatlard.pinata.block;

import justfatlard.pinata.Pinata;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps anchored pinata sheep honest.
 *
 * <p>The block entity discards its sheep in {@code preRemoveSideEffects}, which
 * covers every gameplay removal path (player break, explosion, the pinata's own
 * burst) because those all go through {@code Level.destroyBlock}. It does NOT
 * cover {@code /setblock} and {@code /fill}: those place with
 * {@code Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS} (256) set, which is exactly
 * the flag {@code LevelChunk.setBlockState} checks before calling
 * {@code preRemoveSideEffects}. Structure/worldgen placement and third-party
 * world edits can skip it too.
 *
 * <p>So this tracker sweeps loaded anchor sheep on a timer and discards any whose
 * pinata block is gone, or that a live pinata does not claim (duplicates). It is
 * a self-healing backstop: it also cleans up worlds that already picked up
 * orphans before this check existed.
 */
public final class AnchorSheepTracker {
    private static final int SWEEP_INTERVAL_TICKS = 20;

    // Only anchor sheep land here, so the swept set stays tiny. Keyed by UUID
    // because Entity equality is by network id, which is not unique across levels.
    private static final Map<UUID, Sheep> TRACKED = new HashMap<>();

    private static int tickCounter = 0;

    private AnchorSheepTracker() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> track(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> untrack(entity));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < SWEEP_INTERVAL_TICKS) return;
            tickCounter = 0;
            sweep();
        });
    }

    /** Tracks an entity if it is an anchor sheep. Safe to call on anything. */
    public static void track(Entity entity) {
        if (isAnchorSheep(entity)) {
            TRACKED.put(entity.getUUID(), (Sheep) entity);
        }
    }

    private static void untrack(Entity entity) {
        if (isAnchorSheep(entity)) {
            TRACKED.remove(entity.getUUID());
        }
    }

    private static boolean isAnchorSheep(Entity entity) {
        return entity instanceof Sheep
            && entity.entityTags().contains(PinataBlockEntity.ANCHOR_SHEEP_TAG);
    }

    private static void sweep() {
        if (TRACKED.isEmpty()) return;

        // discard() removes the entity immediately, which fires ENTITY_UNLOAD
        // synchronously and mutates TRACKED. Iterate a snapshot, never the live
        // map, or the sweep takes the server down with a
        // ConcurrentModificationException the first time it finds an orphan.
        for (Sheep sheep : new ArrayList<>(TRACKED.values())) {
            if (sheep.isRemoved() || !(sheep.level() instanceof ServerLevel level)) {
                TRACKED.remove(sheep.getUUID());
                continue;
            }

            if (!isClaimed(level, sheep)) {
                sheep.discard();
                TRACKED.remove(sheep.getUUID());
            }
        }
    }

    /**
     * True if a live pinata block entity at the sheep's position claims it. An
     * unclaimed pinata (block entity with no sheep recorded, e.g. after its data
     * was reset) adopts this sheep rather than leaving both to churn.
     */
    private static boolean isClaimed(ServerLevel level, Sheep sheep) {
        BlockPos pos = BlockPos.containing(sheep.position());
        // The sheep is loaded, so its chunk is loaded: this does not force-load.
        return level.getBlockEntity(pos) instanceof PinataBlockEntity pinata
            && pinata.claims(sheep);
    }

    /**
     * Immediate orphan check for a sheep that just got interacted with, so a hit
     * on a stale sheep cleans it up now instead of up to a sweep later.
     */
    public static boolean discardIfOrphaned(ServerLevel level, Sheep sheep) {
        if (isClaimed(level, sheep)) return false;

        System.out.println("[" + Pinata.MOD_ID + "] Discarded orphaned anchor sheep at "
            + BlockPos.containing(sheep.position()));
        sheep.discard();
        TRACKED.remove(sheep.getUUID());
        return true;
    }
}
