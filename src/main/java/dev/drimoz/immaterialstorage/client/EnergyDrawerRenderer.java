package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
     * The niche is 14 px wide and 10 px deep (models/block/drawer_niche.json). The cube turns on
     * its vertical axis, so its diagonal must fit the depth: 6.5 x sqrt(2) = 9.2 px.
     */
    private static final float CUBE_SCALE = 6.5f / 16f;
    /** Middle of the niche, measured from the front face. */
    private static final float NICHE_CENTRE_DEPTH = 5f / 16f;
    /** The cube sits a little high, leaving room for the amount under it. */
    private static final float CUBE_Y = 1f / 16f;

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
        int light = DrawerRendering.frontLight(drawer);

        long capacity = drawer.energy().getCapacityLong();
        long stored = drawer.energy().getStoredLong();
        float progress = capacity <= 0 ? 0f : (float) Math.min(1d, stored / (double) capacity);

        pose.pushPose();
        DrawerRendering.faceFront(pose, drawer);
        renderCube(drawer, partialTicks, pose, buffers, light, combinedOverlay, progress);
        // stored / capacity: the capacity moves with the upgrades, so stored alone says little.
        DrawerRendering.drawAmount(font, pose, buffers, light,
                StorageDrawersContract.formatCount(stored) + " / " + StorageDrawersContract.formatCount(capacity) + " FE");

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
