package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
     * The amount sits on the front frame's bottom rail (1 to 3 px up, half a pixel in:
     * models/block/drawer_body.json), at Functional Storage's size: 0.007 at most, never wider than
     * 0.4 of the block, so it reads like the amounts on their drawers and on Immaterial Drawers'.
     */
    private static final float RAIL_CENTRE_Y = -6f / 16f;
    private static final float RAIL_Z = -0.5f + 0.5f / 16f - 0.002f;
    private static final float TEXT_SCALE = 0.007f;
    private static final float TEXT_MAX_WIDTH = 0.4f;

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

    /** The block is opaque, so its own light is 0: the tank is lit as the block in front is. */
    static int frontLight(DrawerBlockEntity drawer) {
        Direction facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
        return LevelRenderer.getLightColor(drawer.getLevel(), drawer.getBlockPos().relative(facing));
    }

    /** Writes the amount centred on the rail, shrunk when it would run too wide; full bright, as Functional Storage does. */
    static void drawAmount(Font font, PoseStack pose, MultiBufferSource buffers, String text) {
        float scale = Math.min(TEXT_SCALE, TEXT_MAX_WIDTH / Math.max(1, font.width(text)));
        pose.pushPose();
        // Glyphs are 7 of the line's 9 units high; centre those on the rail.
        pose.translate(0, RAIL_CENTRE_Y + 3.5f * scale, RAIL_Z);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
