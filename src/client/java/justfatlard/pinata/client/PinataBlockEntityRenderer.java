package justfatlard.pinata.client;

import justfatlard.pinata.block.PinataBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

public class PinataBlockEntityRenderer implements BlockEntityRenderer<PinataBlockEntity, PinataRenderState> {
    private static final Identifier SHEEP_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/sheep/sheep.png");
    private static final Identifier SHEEP_WOOL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/sheep/sheep_wool.png");

    private final SheepModel sheepModel;
    private final SheepFurModel woolModel;
    private final SheepRenderState dummySheepState;

    public PinataBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        ModelPart sheepRoot = ctx.bakeLayer(ModelLayers.SHEEP);
        this.sheepModel = new SheepModel(sheepRoot);

        ModelPart woolRoot = ctx.bakeLayer(ModelLayers.SHEEP_WOOL);
        this.woolModel = new SheepFurModel(woolRoot);

        this.dummySheepState = new SheepRenderState();
        this.dummySheepState.scale = 1.0f;
        this.dummySheepState.ageScale = 1.0f;
    }

    @Override
    public PinataRenderState createRenderState() {
        return new PinataRenderState();
    }

    @Override
    public void extractRenderState(PinataBlockEntity entity, PinataRenderState state, float tickProgress,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.tickProgress = tickProgress;
        state.worldTime = entity.getLevel() != null ? entity.getLevel().getGameTime() : 0L;
        state.inCooldown = entity.isInCooldown();
    }

    @Override
    public void submit(PinataRenderState state, PoseStack matrices, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        matrices.pushPose();

        matrices.translate(0.5, 0.0, 0.5);

        float scale = 0.45f;
        matrices.scale(scale, scale, scale);

        matrices.translate(0.0, 1.5, 0.0);
        matrices.mulPose(Axis.XP.rotationDegrees(180.0f));

        float rotation = ((state.worldTime + state.tickProgress) * 2.0f) % 360.0f;
        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        int light = state.lightCoords;

        // Body — use full 10-param overload with explicit white color, no outline
        collector.submitModel(
            sheepModel, dummySheepState, matrices,
            sheepModel.renderType(SHEEP_TEXTURE),
            light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF,
            null, 0, state.breakProgress
        );

        // Wool with rainbow tint (skip when in cooldown = sheared look)
        if (!state.inCooldown) {
            float[] rgb = getJebRainbowColor(state.worldTime, state.tickProgress);
            int packedColor = 0xFF000000
                | ((int)(rgb[0] * 255) << 16)
                | ((int)(rgb[1] * 255) << 8)
                | (int)(rgb[2] * 255);

            collector.submitModel(
                woolModel, dummySheepState, matrices,
                woolModel.renderType(SHEEP_WOOL_TEXTURE),
                light, OverlayTexture.NO_OVERLAY, packedColor,
                null, 0, state.breakProgress
            );
        }

        matrices.popPose();
    }

    private float[] getJebRainbowColor(long worldTime, float tickProgress) {
        int tickRate = 25;
        float time = (worldTime + tickProgress);
        int tick = (int)(time / tickRate);

        DyeColor[] colors = DyeColor.values();
        int colorCount = colors.length;

        int idx1 = tick % colorCount;
        int idx2 = (tick + 1) % colorCount;

        float progress = (time % tickRate) / tickRate;

        float[] color1 = getSheepColorComponents(colors[idx1]);
        float[] color2 = getSheepColorComponents(colors[idx2]);

        float r = Mth.lerp(progress, color1[0], color2[0]);
        float g = Mth.lerp(progress, color1[1], color2[1]);
        float b = Mth.lerp(progress, color1[2], color2[2]);

        return new float[]{r, g, b};
    }

    private float[] getSheepColorComponents(DyeColor color) {
        return switch (color) {
            case WHITE -> new float[]{1.0f, 1.0f, 1.0f};
            case ORANGE -> new float[]{0.85f, 0.52f, 0.2f};
            case MAGENTA -> new float[]{0.7f, 0.36f, 0.67f};
            case LIGHT_BLUE -> new float[]{0.38f, 0.6f, 0.85f};
            case YELLOW -> new float[]{0.95f, 0.82f, 0.21f};
            case LIME -> new float[]{0.49f, 0.73f, 0.18f};
            case PINK -> new float[]{0.95f, 0.5f, 0.65f};
            case GRAY -> new float[]{0.3f, 0.34f, 0.37f};
            case LIGHT_GRAY -> new float[]{0.6f, 0.6f, 0.55f};
            case CYAN -> new float[]{0.15f, 0.48f, 0.55f};
            case PURPLE -> new float[]{0.44f, 0.22f, 0.68f};
            case BLUE -> new float[]{0.19f, 0.22f, 0.63f};
            case BROWN -> new float[]{0.44f, 0.31f, 0.2f};
            case GREEN -> new float[]{0.33f, 0.42f, 0.18f};
            case RED -> new float[]{0.56f, 0.17f, 0.17f};
            case BLACK -> new float[]{0.1f, 0.1f, 0.14f};
        };
    }
}
