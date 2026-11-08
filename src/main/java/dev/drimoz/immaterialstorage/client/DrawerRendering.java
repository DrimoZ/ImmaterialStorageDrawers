package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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
}
