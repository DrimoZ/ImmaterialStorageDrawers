package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlock;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.util.EnergyFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws what an Energy Drawer holds: the energy cube turning in the niche, and the amount below it.
 *
 * <p>The cube and its motion come from Immaterial Drawers, so both mods read the same: a frame that
 * turns at a steady pace, and a translucent, full-bright core that is absent when empty and spins
 * faster and more solid as the drawer fills. The exact figure is the text; the cube is the glance.
 *
 * <p>Each drawer is out of phase with its neighbours, from its position, so a wall of them does not
 * spin in lockstep.
 */
public class EnergyDrawerRenderer implements BlockEntityRenderer<EnergyDrawerBlockEntity> {

    public static final ModelResourceLocation CUBE_FRAME = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "block/energy_cube_frame"));
    public static final ModelResourceLocation CUBE_CORE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "block/energy_cube_core"));

    /**
     * The niche is 14 px wide and 10 px deep (models/block/energy_drawer.json). The cube turns on
     * its vertical axis, so its diagonal must fit the depth: 6.5 x sqrt(2) = 9.2 px.
     */
    private static final float CUBE_SCALE = 6.5f / 16f;
    /** Middle of the niche, measured from the front face. */
    private static final float NICHE_CENTRE_DEPTH = 5f / 16f;
    /** The cube sits a little high, leaving room for the amount under it. */
    private static final float CUBE_Y = 1f / 16f;

    /** Text on the front plane, over the lower part of the niche. */
    private static final float TEXT_Y = -5.5f / 16f;
    private static final float TEXT_SCALE = 0.006f;

    private static final float FRAME_SPEED = 1.2f;
    private static final float CORE_SPEED_EMPTY = 2f;
    private static final float CORE_SPEED_FULL = 12f;
    private static final float BOB_HEIGHT = 0.25f / 16f;
    private static final float BOB_SPEED = 0.045f;
    /** Lower than this and translucent orange over dark stone reads as rust, not a little charge. */
    private static final float CORE_ALPHA_EMPTY = 0.55f;
    private static final Vector3f CORE_AXIS = new Vector3f(1, 1, 1).normalize();

    /** The six faces, then the unculled quads, which is where rotated elements end up. */
    private static final Direction[] SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, null
    };

    private final RandomSource random = RandomSource.create();
    private final Font font;

    public EnergyDrawerRenderer(Font font) {
        this.font = font;
    }

    @Override
    public void render(EnergyDrawerBlockEntity drawer, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int combinedLight, int combinedOverlay) {
        if (drawer.getLevel() == null) {
            return;
        }
        Direction facing = drawer.getBlockState().getValue(EnergyDrawerBlock.FACING);
        // The block is opaque, so its own light is 0: light the niche as the block in front is lit.
        int light = LevelRenderer.getLightColor(drawer.getLevel(), drawer.getBlockPos().relative(facing));

        long capacity = drawer.energy().getCapacityLong();
        long stored = drawer.energy().getStoredLong();
        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, stored / (double) capacity);

        pose.pushPose();
        // Block centre, then turned so that local north (-z) is the drawer's front.
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));

        renderCube(drawer, partialTicks, pose, buffers, light, combinedOverlay, progress);
        renderAmount(pose, buffers, light, stored, capacity);

        pose.popPose();
    }

    private void renderCube(EnergyDrawerBlockEntity drawer, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, float progress) {
        var models = Minecraft.getInstance().getModelManager();
        float time = drawer.getLevel().getGameTime() % 72000L + partialTicks;
        float phase = Math.floorMod(drawer.getBlockPos().asLong() * 31L, 360L);

        pose.pushPose();
        pose.translate(0, CUBE_Y + Mth.sin(time * BOB_SPEED + phase) * BOB_HEIGHT, -0.5f + NICHE_CENTRE_DEPTH);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(time * FRAME_SPEED + phase));
        centreModel(pose);
        renderModel(models.getModel(CUBE_FRAME), pose, buffers.getBuffer(Sheets.cutoutBlockSheet()), 1f, light, overlay);
        pose.popPose();

        if (progress > 0) {
            float speed = Mth.lerp(progress, CORE_SPEED_EMPTY, CORE_SPEED_FULL);
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationAxis((time * speed + phase) * Mth.DEG_TO_RAD, CORE_AXIS));
            centreModel(pose);
            renderModel(models.getModel(CUBE_CORE), pose, buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                    Mth.lerp(progress, CORE_ALPHA_EMPTY, 1f), LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }

        pose.popPose();
    }

    /** "stored / capacity": the capacity moves with the upgrades, so stored alone says little. */
    private void renderAmount(PoseStack pose, MultiBufferSource buffers, int light, long stored, long capacity) {
        String text = EnergyFormat.format(stored) + " / " + EnergyFormat.format(capacity) + " FE";
        pose.pushPose();
        // Just in front of the front plane, then turned to face whoever looks at the front.
        pose.translate(0, TEXT_Y, -0.5f - 0.002f);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }

    /** Models are authored in a 0..16 block; this puts their centre on the pivot, at cube size. */
    private static void centreModel(PoseStack pose) {
        pose.scale(CUBE_SCALE, CUBE_SCALE, CUBE_SCALE);
        pose.translate(-0.5, -0.5, -0.5);
    }

    /** Every quad of a baked model, culled faces included: there is no neighbour to cull against. */
    private void renderModel(BakedModel model, PoseStack pose, VertexConsumer buffer, float alpha, int light, int overlay) {
        PoseStack.Pose last = pose.last();
        for (Direction side : SIDES) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, side, random, ModelData.EMPTY, null)) {
                buffer.putBulkData(last, quad, 1f, 1f, 1f, alpha, light, overlay, false);
            }
        }
    }
}
