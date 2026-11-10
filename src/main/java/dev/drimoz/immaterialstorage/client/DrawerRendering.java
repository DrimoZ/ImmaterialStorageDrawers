package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

/**
 * What every drawer renderer of ours shares: the frame they draw in, the light they use, and the
 * amount written on the front. One place, so every drawer in a wall reads the same.
 */
final class DrawerRendering {

    /**
     * The amounts sit on the front frame (half a pixel in: models/block/drawer_body*.json), where
     * {@link Compartments} places them, at Functional Storage's size: 0.007 at most, never wider than
     * the layout allows, so they read like the amounts on their drawers and on Immaterial Drawers'.
     */
    private static final float FRAME_Z = -0.5f + 0.5f / 16f - 0.002f;
    private static final float TEXT_SCALE = 0.007f;

    private DrawerRendering() {
    }

    /**
     * Moves the pose to the block centre, turned so that local -z is the drawer's front. Local
     * coordinates then run from -0.5 to 0.5; the front plane is z = -0.5.
     */
    static void faceFront(PoseStack pose, DrawerBlockEntity drawer) {
        Direction facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
    }

    /** A position from the viewer's left, in pixels, in the frame of {@link #faceFront} (+x is the viewer's left). */
    static float localX(float fromLeft) {
        return (8 - fromLeft) / 16f;
    }

    /** A height from the bottom, in pixels, in the frame of {@link #faceFront}. */
    static float localY(float fromBottom) {
        return (fromBottom - 8) / 16f;
    }

    /** The block is opaque, so its own light is 0: the tank is lit as the block in front is. */
    static int frontLight(DrawerBlockEntity drawer) {
        Direction facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
        return LevelRenderer.getLightColor(drawer.getLevel(), drawer.getBlockPos().relative(facing));
    }

    /** Writes a compartment's amount, shrunk when it would run too wide; full bright, as Functional Storage does. */
    static void drawAmount(Font font, PoseStack pose, MultiBufferSource buffers, String text,
                           Compartments compartments, int compartment) {
        float scale = Math.min(TEXT_SCALE, compartments.labelMaxWidth() / Math.max(1, font.width(text)));
        pose.pushPose();
        // Glyphs are 7 of the line's 9 units high; centre those on the label's place.
        pose.translate(localX(compartments.labelFromLeft(compartment)),
                localY(compartments.labelFromBottom(compartment)) + 3.5f * scale, FRAME_Z);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * Depth of every tank: Functional Storage's fluid drawer tanks run from just behind the front
     * frame to 14.5 px deep. Their sides come from {@link Compartments}.
     */
    private static final float TANK_FRONT = -0.5f + 1f / 16f;
    private static final float TANK_BACK = -0.5f + 14.5f / 16f - 0.001f;

    /**
     * A compartment's content standing in its tank, as high as it is full: a front face and a
     * surface, in the sprite at its own scale. The colour carries the alpha.
     */
    static void drawTank(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, int color, int light,
                         float progress, Compartments compartments, int compartment) {
        // No culling: the quads are seen from one side only, and a wrong winding would hide them.
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();

        float left = localX(compartments.tankLeft(compartment));
        float right = localX(compartments.tankRight(compartment));
        float bottom = localY(compartments.tankBottom(compartment));
        float height = (compartments.tankTop(compartment) - compartments.tankBottom(compartment)) / 16f;
        float top = bottom + height * Math.max(progress, 1f / 16f);
        float u0 = sprite.getU0();
        float u1 = sprite.getU(left - right);
        float v0 = sprite.getV0();
        float vFront = sprite.getV(Math.min(1f, top - bottom));
        float vTop = sprite.getV(13.5f / 16f);

        // Front face, towards -z.
        vertex(buffer, last, left, bottom, TANK_FRONT, u0, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, right, bottom, TANK_FRONT, u1, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, right, top, TANK_FRONT, u1, v0, color, light, 0, 0, -1);
        vertex(buffer, last, left, top, TANK_FRONT, u0, v0, color, light, 0, 0, -1);

        // Surface, towards +y.
        vertex(buffer, last, right, top, TANK_FRONT, u0, v0, color, light, 0, 1, 0);
        vertex(buffer, last, right, top, TANK_BACK, u0, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, left, top, TANK_BACK, u1, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, left, top, TANK_FRONT, u1, v0, color, light, 0, 1, 0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int color, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static final net.minecraft.resources.ResourceLocation LOCK =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(dev.drimoz.immaterialstorage.ImmaterialStorage.MOD_ID, "block/lock");

    /**
     * The padlock of a drawer locked with Storage Drawers' Drawer Key, as Storage Drawers and Immaterial
     * Drawers show theirs: 2 px, centred on the front frame's top rail, lit like the amounts.
     */
    static void drawLock(PoseStack pose, MultiBufferSource buffers) {
        TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LOCK);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();
        float left = localX(7), right = localX(9), bottom = localY(13), top = localY(15);
        int light = LightTexture.FULL_BRIGHT;
        vertex(buffer, last, left, bottom, FRAME_Z, sprite.getU0(), sprite.getV1(), 0xFFFFFFFF, light, 0, 0, -1);
        vertex(buffer, last, right, bottom, FRAME_Z, sprite.getU1(), sprite.getV1(), 0xFFFFFFFF, light, 0, 0, -1);
        vertex(buffer, last, right, top, FRAME_Z, sprite.getU1(), sprite.getV0(), 0xFFFFFFFF, light, 0, 0, -1);
        vertex(buffer, last, left, top, FRAME_Z, sprite.getU0(), sprite.getV0(), 0xFFFFFFFF, light, 0, 0, -1);
    }
}
