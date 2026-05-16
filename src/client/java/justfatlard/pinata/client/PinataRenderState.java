package justfatlard.pinata.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Render state for the pinata block entity.
 * Holds data extracted from the block entity for use during rendering.
 */
public class PinataRenderState extends BlockEntityRenderState {
    public long worldTime;
    public float tickProgress;
    public boolean inCooldown;
}
