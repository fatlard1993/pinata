package justfatlard.pinata.block;

import justfatlard.pinata.Pinata;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PinataBlockEntity extends BlockEntity {
    /** Command tag marking the decorative sheep this block entity anchors. */
    public static final String ANCHOR_SHEEP_TAG = "pinata_anchor";

    private boolean indestructible = false;
    private int hitsToBreak = 5;
    private int hitsRemaining = 5;

    // Content sets: each set is a list of ItemStack-like entries
    private final List<List<ContentEntry>> contentSets = new ArrayList<>();
    private int currentContentSetIndex = 0;
    private boolean randomizeContentSets = false;
    private double spreadDistance = 0.5;
    private int cooldownTicks = 0;
    private long lastResetTick = 0;

    private final Random random = new Random();

    // The real, decorative "jeb_" sheep this block anchors. UUID is persisted;
    // the direct reference is just a per-load cache.
    private UUID sheepUuid;
    private Sheep sheepCache;

    public PinataBlockEntity(BlockPos pos, BlockState state) {
        super(Pinata.PINATA_BLOCK_ENTITY_TYPE, pos, state);
    }

    /**
     * Server-side ticker: keeps the anchored sheep alive, centered, and in sync
     * with the cooldown state (sheared while depleted). Spawns a fresh sheep if
     * the anchored one is missing (first tick after placement, or after a chunk
     * reload where the entity got lost).
     */
    public static void serverTick(Level world, BlockPos pos, BlockState state, PinataBlockEntity be) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Sheep sheep = be.resolveSheep(serverLevel);
        if (sheep == null) {
            sheep = be.spawnAnchorSheep(serverLevel, pos);
            if (sheep == null) return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        if (sheep.isPassenger()) sheep.stopRiding();
        if (sheep.position().distanceToSqr(x, y, z) > 1.0E-4 || sheep.getYRot() != 0.0f) {
            sheep.snapTo(x, y, z, 0.0f, 0.0f);
            sheep.setYBodyRot(0.0f);
            sheep.setYHeadRot(0.0f);
            sheep.setDeltaMovement(Vec3.ZERO);
        }

        if (sheep.isOnFire()) sheep.clearFire();
        if (sheep.getHealth() < sheep.getMaxHealth()) sheep.setHealth(sheep.getMaxHealth());

        // Cooldown visual: a depleted pinata is a sheared sheep. Wool "regrows"
        // when the cooldown ends (NoAI sheep never eat grass, so set it manually).
        boolean depleted = be.isInCooldown();
        if (sheep.isSheared() != depleted) sheep.setSheared(depleted);
    }

    private Sheep resolveSheep(ServerLevel world) {
        if (sheepCache != null && sheepCache.isAlive() && sheepCache.level() == world) {
            return sheepCache;
        }
        sheepCache = null;
        if (sheepUuid != null && world.getEntity(sheepUuid) instanceof Sheep sheep && sheep.isAlive()) {
            sheepCache = sheep;
        }
        return sheepCache;
    }

    private Sheep spawnAnchorSheep(ServerLevel world, BlockPos pos) {
        Sheep sheep = EntityTypes.SHEEP.create(world, EntitySpawnReason.TRIGGERED);
        if (sheep == null) return null;

        sheep.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        sheep.setYBodyRot(0.0f);
        sheep.setYHeadRot(0.0f);
        sheep.setNoAi(true);
        sheep.setSilent(true);
        sheep.setPersistenceRequired();
        // Vanilla clients render the rainbow wool cycle natively via the jeb_
        // easter egg (MobRenderer.checkMagicName reads getCustomName(), so the
        // hidden name still triggers it).
        sheep.setCustomName(Component.literal("jeb_"));
        sheep.setCustomNameVisible(false);
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(isInCooldown());
        sheep.addTag(ANCHOR_SHEEP_TAG);

        if (!world.addFreshEntity(sheep)) {
            System.err.println("[" + Pinata.MOD_ID + "] Failed to spawn anchor sheep at " + pos);
            return null;
        }

        sheepUuid = sheep.getUUID();
        sheepCache = sheep;
        setChanged();
        AnchorSheepTracker.track(sheep);
        return sheep;
    }

    /**
     * Whether this pinata owns the given sheep. An unclaimed pinata (no sheep
     * recorded yet) adopts it, so a block whose stored UUID was lost reuses the
     * sheep already standing on it instead of stacking up a second one.
     */
    public boolean claims(Sheep sheep) {
        if (sheepUuid == null) {
            sheepUuid = sheep.getUUID();
            sheepCache = sheep;
            setChanged();
            return true;
        }
        return sheepUuid.equals(sheep.getUUID());
    }

    /**
     * Called from LevelChunk.setBlockState when the block is removed, but only
     * when the caller did not set Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
     * (256). That covers every gameplay path (mined, exploded, the pinata's own
     * burst, all via Level.destroyBlock) but NOT /setblock and /fill, which
     * always set that flag; AnchorSheepTracker is the backstop for those.
     *
     * <p>Not called on chunk unload, so the sheep persists with the chunk and is
     * reacquired by UUID on reload.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) {
            Sheep sheep = resolveSheep(serverLevel);
            if (sheep != null) sheep.discard();
            sheepCache = null;
        }
    }

    public boolean isIndestructible() {
        return indestructible;
    }

    public void setIndestructible(boolean indestructible) {
        this.indestructible = indestructible;
        setChanged();
        syncToClients();
    }

    public int getHitsToBreak() {
        return hitsToBreak;
    }

    public void setHitsToBreak(int hits) {
        this.hitsToBreak = hits;
        this.hitsRemaining = hits;
        setChanged();
    }

    public int getHitsRemaining() {
        return hitsRemaining;
    }

    public void setRandomizeContentSets(boolean randomize) {
        this.randomizeContentSets = randomize;
        setChanged();
    }

    public boolean isRandomizeContentSets() {
        return randomizeContentSets;
    }

    public double getSpreadDistance() {
        return spreadDistance;
    }

    public void setSpreadDistance(double spread) {
        this.spreadDistance = Math.max(0.0, Math.min(10.0, spread));
        setChanged();
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = Math.max(0, ticks);
        setChanged();
        syncToClients();
    }

    public boolean isInCooldown() {
        if (!indestructible || cooldownTicks <= 0 || level == null) return false;
        return level.getGameTime() - lastResetTick < cooldownTicks;
    }

    public void clearContentSets() {
        contentSets.clear();
        currentContentSetIndex = 0;
        setChanged();
    }

    public void addContentSet(List<ContentEntry> contents) {
        contentSets.add(new ArrayList<>(contents));
        setChanged();
    }

    public List<List<ContentEntry>> getContentSets() {
        return contentSets;
    }

    public int getCurrentContentSetIndex() {
        return currentContentSetIndex;
    }

    /** Handles one pinata hit. {@code player} may be null (e.g. routed non-player damage). */
    public void onHit(Level world, BlockPos pos, Player player) {
        if (world.isClientSide()) return;

        if (indestructible && cooldownTicks > 0 && world.getGameTime() - lastResetTick < cooldownTicks) {
            PinataBlock.spawnCooldownParticles(world, pos);
            PinataBlock.playCooldownSound(world, pos);
            return;
        }

        PinataBlock.spawnHitParticles(world, pos);
        PinataBlock.playHitSound(world, pos);

        hitsRemaining--;
        setChanged();

        if (hitsRemaining <= 0) {
            dropCurrentContents(world, pos);
            PinataBlock.playBreakSound(world, pos);

            PinataBlock.spawnBurstParticles(world, pos);

            if (indestructible) {
                hitsRemaining = hitsToBreak;
                advanceContentSet();
                lastResetTick = world.getGameTime();
                syncToClients();
            } else {
                world.destroyBlock(pos, false);
            }
        }
    }

    private void dropCurrentContents(Level world, BlockPos pos) {
        if (contentSets.isEmpty()) return;

        int idx = currentContentSetIndex % contentSets.size();
        List<ContentEntry> contents = contentSets.get(idx);

        for (ContentEntry entry : contents) {
            Item item = BuiltInRegistries.ITEM.getValue(entry.itemId());
            if (item == null) continue;

            // Spawn each item individually for spray effect
            for (int i = 0; i < entry.count(); i++) {
                ItemStack stack = new ItemStack(item, 1);
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * spreadDistance * 0.3;
                double x = pos.getX() + 0.5 + Math.cos(angle) * dist;
                double y = pos.getY() + 0.8;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * dist;
                ItemEntity itemEntity = new ItemEntity(world, x, y, z, stack);
                double speed = 0.15 + random.nextDouble() * spreadDistance * 0.4;
                double launchAngle = random.nextDouble() * Math.PI * 2;
                itemEntity.setDeltaMovement(
                    Math.cos(launchAngle) * speed,
                    0.3 + random.nextDouble() * 0.4,
                    Math.sin(launchAngle) * speed
                );
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
            }
        }
    }

    private void advanceContentSet() {
        if (contentSets.isEmpty()) return;

        if (randomizeContentSets) {
            currentContentSetIndex = random.nextInt(contentSets.size());
        } else {
            currentContentSetIndex = (currentContentSetIndex + 1) % contentSets.size();
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Indestructible", indestructible);
        output.putInt("HitsToBreak", hitsToBreak);
        output.putInt("HitsRemaining", hitsRemaining);
        output.putInt("CurrentContentSetIndex", currentContentSetIndex);
        output.putBoolean("RandomizeContentSets", randomizeContentSets);
        output.putDouble("SpreadDistance", spreadDistance);
        output.putInt("CooldownTicks", cooldownTicks);
        output.putLong("LastResetTick", lastResetTick);
        output.storeNullable("SheepUuid", UUIDUtil.CODEC, sheepUuid);

        ValueOutput.ValueOutputList setsList = output.childrenList("ContentSets");
        for (List<ContentEntry> set : contentSets) {
            ValueOutput setOutput = setsList.addChild();
            ValueOutput.ValueOutputList entriesList = setOutput.childrenList("Entries");
            for (ContentEntry entry : set) {
                ValueOutput entryOutput = entriesList.addChild();
                entryOutput.putString("Item", entry.itemId().toString());
                entryOutput.putInt("Count", entry.count());
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        indestructible = input.getBooleanOr("Indestructible", false);
        hitsToBreak = input.getIntOr("HitsToBreak", 5);
        hitsRemaining = input.getIntOr("HitsRemaining", 5);
        currentContentSetIndex = input.getIntOr("CurrentContentSetIndex", 0);
        randomizeContentSets = input.getBooleanOr("RandomizeContentSets", false);
        spreadDistance = input.getDoubleOr("SpreadDistance", 0.5);
        cooldownTicks = input.getIntOr("CooldownTicks", 0);
        lastResetTick = input.getLongOr("LastResetTick", 0);
        sheepUuid = input.read("SheepUuid", UUIDUtil.CODEC).orElse(null);
        sheepCache = null;

        contentSets.clear();
        for (ValueInput setInput : input.childrenListOrEmpty("ContentSets")) {
            List<ContentEntry> set = new ArrayList<>();
            for (ValueInput entryInput : setInput.childrenListOrEmpty("Entries")) {
                String itemIdStr = entryInput.getStringOr("Item", "minecraft:air");
                int count = entryInput.getIntOr("Count", 1);
                Identifier itemId = Identifier.parse(itemIdStr);
                set.add(new ContentEntry(itemId, count));
            }
            contentSets.add(set);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public record ContentEntry(Identifier itemId, int count) {
    }
}
