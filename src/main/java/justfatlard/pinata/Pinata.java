package justfatlard.pinata;

import justfatlard.pinata.block.PinataBlock;
import justfatlard.pinata.block.PinataBlockEntity;
import justfatlard.pinata.command.PinataCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
        Registry.register(BuiltInRegistries.BLOCK, PINATA_BLOCK_ID, PINATA_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, PINATA_BLOCK_ID, PINATA_BLOCK_ITEM);

        PINATA_BLOCK_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "pinata"),
            FabricBlockEntityTypeBuilder.create(PinataBlockEntity::new, PINATA_BLOCK).build()
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PinataCommand.register(dispatcher);
        });

        System.out.println("[" + MOD_ID + "] Loaded successfully");
    }
}
