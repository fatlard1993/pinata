package justfatlard.pinata;

import justfatlard.pinata.block.AnchorSheepTracker;
import justfatlard.pinata.block.PinataBlock;
import justfatlard.pinata.block.PinataBlockEntity;
import justfatlard.pinata.command.PinataCommand;
import justfatlard.pandorical.api.BlockRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Pinata implements ModInitializer {
    public static final String MOD_ID = "pinata";

    public static final Identifier PINATA_BLOCK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "pinata");

    public static final ResourceKey<Block> PINATA_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, PINATA_BLOCK_ID);
    public static final ResourceKey<Item> PINATA_ITEM_KEY = ResourceKey.create(Registries.ITEM, PINATA_BLOCK_ID);

    public static final Block PINATA_BLOCK = new PinataBlock(
        BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.WOOL)
            .noOcclusion()
            .setId(PINATA_BLOCK_KEY)
    );

    public static final BlockItem PINATA_BLOCK_ITEM = new BlockItem(
        PINATA_BLOCK,
        new Item.Properties()
            .setId(PINATA_ITEM_KEY)
            .useBlockDescriptionPrefix()
    );

    public static BlockEntityType<PinataBlockEntity> PINATA_BLOCK_ENTITY_TYPE;

    @Override
    public void onInitialize() {
        // The crowd is re-gathered on a tick because villagers are brain-driven:
        // a schedule behaviour will talk a child out of standing anywhere unless
        // something keeps saying otherwise. It stops on its own when the pinata
        // is broken, because nothing is left to gather around.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!PinataParty.shouldGatherThisTick(server.overworld().getGameTime())) return;

            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                PinataParty.tickParties(level);
            }
        });

        // Guarded class load: PinataDialogue names village-quests types.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("village-quests-justfatlard")) {
            justfatlard.pinata.integration.PinataDialogue.register();
        }

        // Pandorical content sync: lets Pandorical clients register the block/item
        // and assets locally so no pinata client jar is needed.
        PandoricalApi.content().registerBlock(MOD_ID + ":pinata", new BlockRegistration()
            .baseBlock("minecraft:white_wool")
            .model(MOD_ID + ":block/pinata"));
        PandoricalApi.content().registerItem(MOD_ID + ":pinata", new ItemRegistration()
            .model(MOD_ID + ":item/pinata"));
        PandoricalApi.content().registerModAssets(MOD_ID);

        Registry.register(BuiltInRegistries.BLOCK, PINATA_BLOCK_ID, PINATA_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, PINATA_BLOCK_ID, PINATA_BLOCK_ITEM);

        PINATA_BLOCK_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "pinata"),
            FabricBlockEntityTypeBuilder.create(PinataBlockEntity::new, PINATA_BLOCK).build()
        );

        registerAnchorSheepEvents();
        AnchorSheepTracker.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PinataCommand.register(dispatcher);
        });

        System.out.println("[" + MOD_ID + "] Loaded successfully");
    }

    /**
     * The pinata's visual is a real, anchored jeb_ sheep. All damage to it is
     * cancelled; hits with a living attacker are routed to the block entity's
     * pinata logic, with the vanilla hurt flash/wobble broadcast as feedback.
     */
    private static void registerAnchorSheepEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof Sheep sheep)
                || !sheep.entityTags().contains(PinataBlockEntity.ANCHOR_SHEEP_TAG)) {
                return true;
            }

            if (!(sheep.level() instanceof ServerLevel world)) return false;

            // A sheep whose pinata block is gone (or that a pinata does not claim)
            // gets cleaned up on the spot rather than absorbing hits forever.
            if (AnchorSheepTracker.discardIfOrphaned(world, sheep)) return false;

            BlockPos pos = BlockPos.containing(sheep.position());
            if (world.getBlockEntity(pos) instanceof PinataBlockEntity pinata
                && source.getEntity() instanceof LivingEntity attacker) {
                // Cancelled damage suppresses the vanilla hurt animation, so
                // broadcast it explicitly (flash + wobble, no actual damage).
                world.broadcastDamageEvent(sheep, source);
                pinata.onHit(world, pos, attacker instanceof Player player ? player : null);
            }
            return false;
        });

        // Block right-click interactions (shears, dye, wheat, leads, name tags):
        // the anchor sheep is decor, not livestock.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof Sheep sheep
                && sheep.entityTags().contains(PinataBlockEntity.ANCHOR_SHEEP_TAG)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
