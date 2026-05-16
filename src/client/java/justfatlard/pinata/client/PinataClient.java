package justfatlard.pinata.client;

import justfatlard.pinata.Pinata;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class PinataClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(Pinata.PINATA_BLOCK_ENTITY_TYPE, PinataBlockEntityRenderer::new);
        System.out.println("[" + Pinata.MOD_ID + "] Client initialized");
    }
}
