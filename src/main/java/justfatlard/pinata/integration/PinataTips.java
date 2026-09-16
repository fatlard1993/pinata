package justfatlard.pinata.integration;

import justfatlard.block_tip.api.BlockTipApi;
import justfatlard.pinata.Pinata;
import justfatlard.pinata.block.PinataBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.sheep.Sheep;

/**
 * What block-tip says about a piñata. The anchor sheep stands inside the block wearing its rainbow
 * wool, so a crosshair on the piñata lands on the sheep first: tagged as a stand-in, block-tip
 * names the block it is standing in instead. Tagged at load rather than at spawn so the sheep
 * that were already in the world catch up on their own.
 */
public final class PinataTips {
    private PinataTips() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Sheep && entity.entityTags().contains(PinataBlockEntity.ANCHOR_SHEEP_TAG)) {
                entity.addTag(BlockTipApi.STAND_IN);
            }
        });

        BlockTipApi.describe((level, pos, state, player) -> {
            if (!state.is(Pinata.PINATA_BLOCK)) return null;
            if (!(level.getBlockEntity(pos) instanceof PinataBlockEntity pinata)) return null;
            if (!pinata.isIndestructible()) return null;
            return pinata.isInCooldown() ? "Recovering" : "Ready to smash";
        });
    }
}
